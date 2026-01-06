package no.nav.dagpenger.saksbehandling.db.klage

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UgyldigTilstandException
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningerMapper.tilJson
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningerMapper.tilKlageOpplysninger
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageBehandlingUtført
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES_AV_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLING_UTFORT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandslogg
import no.nav.dagpenger.saksbehandling.klage.KlageinstansVedtak
import no.nav.dagpenger.saksbehandling.serder.tilHendelse
import no.nav.dagpenger.saksbehandling.serder.tilJson
import org.postgresql.util.PGobject
import java.util.UUID
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class PostgresKlageRepository(
    private val datasource: DataSource,
) : KlageRepository {
    override fun lagre(klageBehandling: KlageBehandling) {
        sessionOf(datasource).use { session ->
            session.transaction { tx ->
                tx.lagre(klageBehandling)
            }
        }
    }

    override fun hentKlageBehandling(behandlingId: UUID): KlageBehandling =
        finnKlageBehandling(behandlingId) ?: throw DataNotFoundException("Fant ikke klage med id $behandlingId")

    private fun finnKlageBehandling(behandlingId: UUID): KlageBehandling? {
        val klageBehandling =
            sessionOf(datasource).use { session ->
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            SELECT klag.id AS behandling_id, 
                                   klag.tilstand, 
                                   klag.journalpost_id, 
                                   klag.behandlende_enhet, 
                                   klag.opplysninger,
                                   beha.opprettet,
                                   kave.id as ka_vedtak_id,
                                   kave.journalpost_ider ,
                                   kave.avsluttet,
                                   kave.utfall,
                                   kave.type as ka_vedtak_type
                            FROM   klage_v1       klag
                            JOIN   behandling_v1  beha ON beha.id = klag.id
                            LEFT JOIN klageinstans_vedtak_v1 kave ON kave.klage_id = klag.id
                            WHERE  klag.id = :behandling_id
                            """.trimIndent(),
                        paramMap = mapOf("behandling_id" to behandlingId),
                    ).map { row ->
                        row.rehydrerKlageBehandling(datasource)
                    }.asSingle,
                )
            }
        return klageBehandling
    }

    private fun TransactionalSession.lagre(klageBehandling: KlageBehandling) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO klage_v1
                        (id, tilstand, journalpost_id, behandlende_enhet, opplysninger)
                    VALUES
                        (:id, :tilstand, :journalpost_id, :behandlende_enhet, :opplysninger)
                    ON CONFLICT(id) DO UPDATE SET
                     tilstand = :tilstand,
                     journalpost_id = :journalpost_id,
                     behandlende_enhet = :behandlende_enhet,
                     opplysninger = :opplysninger
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to klageBehandling.behandlingId,
                        "tilstand" to klageBehandling.tilstand().type.name,
                        "journalpost_id" to klageBehandling.journalpostId(),
                        "behandlende_enhet" to klageBehandling.behandlendeEnhet(),
                        "opplysninger" to
                            PGobject().also {
                                it.type = "JSONB"
                                it.value = klageBehandling.alleOpplysninger().tilJson()
                            },
                    ),
            ).asUpdate,
        )
        lagre(behandlingId = klageBehandling.behandlingId, tilstandslogg = klageBehandling.tilstandslogg)
        lagre(behandlingId = klageBehandling.behandlingId, klageinstansVedtak = klageBehandling.klageinstansVedtak())
    }

    private fun TransactionalSession.lagre(
        behandlingId: UUID,
        tilstandslogg: KlageTilstandslogg,
    ) {
        tilstandslogg.forEach { tilstandsendring ->
            this.lagre(behandlingId, tilstandsendring)
        }
    }

    private fun TransactionalSession.lagre(
        behandlingId: UUID,
        klageinstansVedtak: KlageinstansVedtak?,
    ) {
        klageinstansVedtak?.let { _ ->
            val journalpostIderArray = this.createArrayOf("text", klageinstansVedtak.journalpostIder)
            this.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO klageinstans_vedtak_v1
                            (id, type,  klage_id, utfall, avsluttet, journalpost_ider)
                        VALUES
                            (:id, :type, :klage_id, :utfall, :avsluttet, :journalpost_ider)
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "id" to klageinstansVedtak.id,
                            "type" to klageinstansVedtak.javaClass.simpleName,
                            "klage_id" to behandlingId,
                            "utfall" to klageinstansVedtak.utfall(),
                            "avsluttet" to klageinstansVedtak.avsluttet,
                            "journalpost_ider" to journalpostIderArray,
                        ),
                ).asUpdate,
            )
        }
    }

    private fun TransactionalSession.lagre(
        behandlingId: UUID,
        tilstandsendring: Tilstandsendring<KlageTilstand.Type>,
    ) {
        this.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO klage_tilstand_logg_v1
                        (id, klage_id, tilstand, hendelse_type, hendelse, tidspunkt)
                    VALUES
                        (:id, :klage_id, :tilstand,:hendelse_type, :hendelse, :tidspunkt)
                    ON CONFLICT DO NOTHING
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to tilstandsendring.id,
                        "klage_id" to behandlingId,
                        "tilstand" to tilstandsendring.tilstand.name,
                        "hendelse_type" to tilstandsendring.hendelse.javaClass.simpleName,
                        "hendelse" to
                            PGobject().also {
                                it.type = "JSONB"
                                it.value = tilstandsendring.hendelse.tilJson()
                            },
                        "tidspunkt" to tilstandsendring.tidspunkt,
                    ),
            ).asUpdate,
        )
    }

    private fun Row.rehydrerKlageBehandling(dataSource: DataSource): KlageBehandling {
        val behandlingId = this.uuid("behandling_id")
        val tilstandAsText = this.string("tilstand")
        val tilstand =
            kotlin
                .runCatching {
                    when (KlageTilstand.Type.valueOf(tilstandAsText)) {
                        BEHANDLES -> KlageBehandling.Behandles
                        OVERSEND_KLAGEINSTANS -> KlageBehandling.OversendKlageinstans
                        FERDIGSTILT -> KlageBehandling.Ferdigstilt
                        AVBRUTT -> KlageBehandling.Avbrutt
                        BEHANDLING_UTFORT -> KlageBehandling.BehandlingUtført
                        BEHANDLES_AV_KLAGEINSTANS -> KlageBehandling.BehandlesAvKlageinstans
                    }
                }.getOrElse { t ->
                    throw UgyldigTilstandException("Kunne ikke rehydrere klagebehandling til tilstand: ${string("tilstand")} ${t.message}")
                }
        val journalpostId = this.stringOrNull("journalpost_id")
        val behandlendeEnhet = this.stringOrNull("behandlende_enhet")
        val opplysninger = this.string("opplysninger").tilKlageOpplysninger()
        val tilstandslogg =
            hentKlageTilstandslogg(
                behandlingId = behandlingId,
                dataSource = dataSource,
            )
        val opprettet = this.localDateTime("opprettet")
        val kavVedtak = this.kaVedtakOrNull()

        return KlageBehandling.rehydrer(
            behandlingId = behandlingId,
            tilstand = tilstand,
            journalpostId = journalpostId,
            behandlendeEnhet = behandlendeEnhet,
            opplysninger = opplysninger,
            tilstandslogg = tilstandslogg,
            opprettet = opprettet,
            klageinstansVedtak = kavVedtak,
        )
    }

    private fun Row.kaVedtakOrNull(): KlageinstansVedtak? =
        this.stringOrNull("ka_vedtak_type")?.let { vedtakType ->
            when (vedtakType) {
                "Klage" -> {
                    KlageinstansVedtak.Klage(
                        id = this.uuid("ka_vedtak_id"),
                        journalpostIder = this.stringList("journalpost_ider"),
                        avsluttet = this.localDateTime("avsluttet"),
                        utfall = KlageinstansVedtak.Klage.Utfall.valueOf(this.string("utfall")),
                    )
                }

                else -> {
                    logger.error { " Ukjent vedtakstype $vedtakType" }
                    throw IllegalArgumentException("Ukjent vedtakstype $vedtakType")
                }
            }
        }

    private fun Row.stringList(columnLabel: String): List<String> = this.array<String>(columnLabel).toList()

    private fun hentKlageTilstandslogg(
        behandlingId: UUID,
        dataSource: DataSource,
    ): KlageTilstandslogg =
        sessionOf(dataSource).use { session ->
            session
                .run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            SELECT id, klage_id, tilstand, hendelse_type, hendelse, tidspunkt
                            FROM   klage_tilstand_logg_v1
                            WHERE  klage_id = :behandling_id
                            """.trimIndent(),
                        paramMap = mapOf("behandling_id" to behandlingId),
                    ).map { row ->
                        Tilstandsendring(
                            id = row.uuid("id"),
                            tilstand = KlageTilstand.Type.valueOf(row.string("tilstand")),
                            hendelse =
                                rehydrerKlageTilstandsendringHendelse(
                                    hendelseType = row.string("hendelse_type"),
                                    hendelseJson = row.string("hendelse"),
                                ),
                            tidspunkt = row.localDateTime("tidspunkt"),
                        )
                    }.asList,
                ).let { KlageTilstandslogg(it) }
        }

    private fun rehydrerKlageTilstandsendringHendelse(
        hendelseType: String,
        hendelseJson: String,
    ): Hendelse =
        when (hendelseType) {
            "KlageMottattHendelse" -> hendelseJson.tilHendelse<KlageMottattHendelse>()
            "ManuellKlageMottattHendelse" -> hendelseJson.tilHendelse<ManuellKlageMottattHendelse>()
            "OversendtKlageinstansHendelse" -> hendelseJson.tilHendelse<OversendtKlageinstansHendelse>()
            "KlageBehandlingUtført" -> hendelseJson.tilHendelse<KlageBehandlingUtført>()
            "AvbruttHendelse" -> hendelseJson.tilHendelse<AvbruttHendelse>()
            "UtsendingDistribuert" -> hendelseJson.tilHendelse<UtsendingDistribuert>()
            else -> {
                logger.error { "rehydrerKlageTilstandsendringHendelse: Ukjent hendelse med type $hendelseType" }
                sikkerlogger.error { "rehydrerKlageTilstandsendringHendelse: Ukjent hendelse med type $hendelseType: $hendelseJson" }
                throw IllegalArgumentException("Ukjent hendelsetype $hendelseType")
            }
        }
}
