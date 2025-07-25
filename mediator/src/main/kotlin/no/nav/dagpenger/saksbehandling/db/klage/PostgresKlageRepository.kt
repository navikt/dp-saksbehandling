package no.nav.dagpenger.saksbehandling.db.klage

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UgyldigTilstandException
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningerMapper.tilJson
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningerMapper.tilKlageOpplysninger
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandsendring
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandslogg
import no.nav.dagpenger.saksbehandling.serder.tilHendelse
import no.nav.dagpenger.saksbehandling.serder.tilJson
import org.postgresql.util.PGobject
import java.util.UUID
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class PostgresKlageRepository(private val datasource: DataSource) : KlageRepository {
    override fun lagre(klageBehandling: KlageBehandling) {
        sessionOf(datasource).use { session ->
            session.transaction { tx ->
                tx.lagre(klageBehandling)
            }
        }
    }

    override fun hentKlageBehandling(behandlingId: UUID): KlageBehandling {
        return finnKlageBehandling(behandlingId) ?: throw DataNotFoundException("Fant ikke klage med id $behandlingId")
    }

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
                                   beha.opprettet
                            FROM   klage_v1       klag
                            JOIN   behandling_v1  beha ON beha.id = klag.id
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
        tilstandsendring: KlageTilstandsendring,
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

        return KlageBehandling.rehydrer(
            behandlingId = behandlingId,
            tilstand = tilstand,
            journalpostId = journalpostId,
            behandlendeEnhet = behandlendeEnhet,
            opplysninger = opplysninger,
            tilstandslogg = tilstandslogg,
            opprettet = opprettet,
        )
    }

    private fun hentKlageTilstandslogg(
        behandlingId: UUID,
        dataSource: DataSource,
    ): KlageTilstandslogg {
        return sessionOf(dataSource).use { session ->
            session.run(
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
                    KlageTilstandsendring(
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
            ).let { KlageTilstandslogg.rehydrer(it) }
        }
    }

    private fun rehydrerKlageTilstandsendringHendelse(
        hendelseType: String,
        hendelseJson: String,
    ): Hendelse {
        return when (hendelseType) {
            "KlageMottattHendelse" -> hendelseJson.tilHendelse<KlageMottattHendelse>()
            "ManuellKlageMottattHendelse" -> hendelseJson.tilHendelse<ManuellKlageMottattHendelse>()
            "OversendtKlageinstansHendelse" -> hendelseJson.tilHendelse<OversendtKlageinstansHendelse>()
            "KlageFerdigbehandletHendelse" -> hendelseJson.tilHendelse<KlageFerdigbehandletHendelse>()
            "AvbruttHendelse" -> hendelseJson.tilHendelse<AvbruttHendelse>()
            else -> {
                logger.error { "rehydrerKlageTilstandsendringHendelse: Ukjent hendelse med type $hendelseType" }
                sikkerlogger.error { "rehydrerKlageTilstandsendringHendelse: Ukjent hendelse med type $hendelseType: $hendelseJson" }
                throw IllegalArgumentException("Ukjent hendelsetype $hendelseType")
            }
        }
    }
}
