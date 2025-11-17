package no.nav.dagpenger.saksbehandling.db.klage

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.RettTilDagpengerOppgave.RettTilDagpengerTilstand.UgyldigTilstandException
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningerMapper.tilJson
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningerMapper.tilKlageOpplysninger
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.klage.Klage
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand.Type.OVERSENDT_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandslogg
import no.nav.dagpenger.saksbehandling.serder.tilHendelse
import no.nav.dagpenger.saksbehandling.serder.tilJson
import org.postgresql.util.PGobject
import java.util.UUID
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class PostgresKlageRepository(private val datasource: DataSource) : KlageRepository {
    override fun lagre(klage: Klage) {
        sessionOf(datasource).use { session ->
            session.transaction { tx ->
                tx.lagre(klage)
            }
        }
    }

    override fun hentKlageBehandling(behandlingId: UUID): Klage {
        return finnKlageBehandling(behandlingId) ?: throw DataNotFoundException("Fant ikke klage med id $behandlingId")
    }

    private fun finnKlageBehandling(behandlingId: UUID): Klage? {
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

    private fun TransactionalSession.lagre(klage: Klage) {
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
                        "id" to klage.behandlingId,
                        "tilstand" to klage.tilstand().type.name,
                        "journalpost_id" to klage.journalpostId(),
                        "behandlende_enhet" to klage.behandlendeEnhet(),
                        "opplysninger" to
                            PGobject().also {
                                it.type = "JSONB"
                                it.value = klage.alleOpplysninger().tilJson()
                            },
                    ),
            ).asUpdate,
        )
        lagre(behandlingId = klage.behandlingId, tilstandslogg = klage.tilstandslogg)
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

    private fun Row.rehydrerKlageBehandling(dataSource: DataSource): Klage {
        val behandlingId = this.uuid("behandling_id")
        val tilstandAsText = this.string("tilstand")
        val tilstand =
            kotlin
                .runCatching {
                    when (KlageTilstand.Type.valueOf(tilstandAsText)) {
                        BEHANDLES -> Klage.Behandles
                        OVERSEND_KLAGEINSTANS -> Klage.OversendKlageinstans
                        OVERSENDT_KLAGEINSTANS -> Klage.OversendtKlageinstans
                        AVBRUTT -> Klage.Avbrutt
                        FERDIGSTILT -> Klage.Ferdigstilt
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

        return Klage.rehydrer(
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
