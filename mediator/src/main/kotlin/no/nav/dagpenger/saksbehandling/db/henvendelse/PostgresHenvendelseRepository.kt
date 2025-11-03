package no.nav.dagpenger.saksbehandling.db.henvendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.Type
import no.nav.dagpenger.saksbehandling.henvendelse.HenvendelseTilstandslogg
import no.nav.dagpenger.saksbehandling.serder.tilHendelse
import java.util.UUID
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class PostgresHenvendelseRepository(private val dataSource: DataSource) : HenvendelseRepository {
    override fun lagre(henvendelse: Henvendelse) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagre(henvendelse)
            }
        }
    }

    override fun hent(henvendelseId: UUID): Henvendelse {
        return finnHenvendelse(henvendelseId)
            ?: throw DataNotFoundException("Kan ikke finne henvendelse med id $henvendelseId")
    }

    private fun finnHenvendelse(henvendelseId: UUID): Henvendelse? {
        val tilstandLoggg = hentTilstandsloggForHenvendelse(henvendelseId)
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT * 
                        FROM   henvendelse_v1
                        WHERE  id = :henvendelse_id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "henvendelse_id" to henvendelseId,
                        ),
                ).map { row ->
                    row.rehydrerHenvendelse(tilstandLoggg)
                }.asSingle,
            )
        }
    }

    private fun Row.rehydrerHenvendelse(tilstandLoggg: HenvendelseTilstandslogg): Henvendelse {
        val henvendelseId = this.uuid("id")
        val tilstand =
            runCatching {
                when (Type.valueOf(value = string("tilstand"))) {
                    Type.KLAR_TIL_BEHANDLING -> Henvendelse.Tilstand.KlarTilBehandling
                    Type.UNDER_BEHANDLING -> Henvendelse.Tilstand.UnderBehandling
                    Type.FERDIGBEHANDLET -> Henvendelse.Tilstand.Ferdigbehandlet
                }
            }.getOrElse { t ->
                throw RuntimeException("Kunne ikke rehydrere henvendelse til tilstand: ${string("tilstand")} ${t.message}")
            }

        return Henvendelse.rehydrer(
            henvendelseId = henvendelseId,
            person =
                Person(
                    id = this.uuid("person_id"),
                    ident = this.string("ident"),
                    skjermesSomEgneAnsatte = this.boolean("skjermesSomEgneAnsatte"),
                    adressebeskyttelseGradering = AdressebeskyttelseGradering.valueOf(this.string("adressebeskyttelse")),
                ),
            journalpostId = this.string("journalpost_id"),
            mottatt = this.localDateTime("mottatt"),
            skjemaKode = this.string("skjemaKode"),
            kategori = Kategori.valueOf(this.string("kategori")),
            behandlerIdent = this.stringOrNull("behandler_ident"),
            tilstand = tilstand,
            tilstandslogg = tilstandLoggg,
        )
    }

    private fun hentTilstandsloggForHenvendelse(henvendelseId: UUID): HenvendelseTilstandslogg {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT id, henvendelse_id, tilstand, hendelse_type, hendelse, tidspunkt
                        FROM   henvendelse_tilstand_logg_v1
                        WHERE  henvendelse_id = :henvendelse_id
                        """.trimIndent(),
                    paramMap = mapOf("henvendelse_id" to henvendelseId),
                ).map { row ->
                    Tilstandsendring(
                        id = row.uuid("id"),
                        tilstand = Type.valueOf(row.string("tilstand")),
                        hendelse =
                            rehydrerTilstandsendringHendelse(
                                hendelseType = row.string("hendelse_type"),
                                hendelseJson = row.string("hendelse"),
                            ),
                        tidspunkt = row.localDateTime("tidspunkt"),
                    )
                }.asList,
            ).let { HenvendelseTilstandslogg(it) }
        }
    }

    private fun rehydrerTilstandsendringHendelse(
        hendelseType: String,
        hendelseJson: String,
    ): Hendelse {
        return when (hendelseType) {
            "HenvendelseMottattHendelse" -> hendelseJson.tilHendelse<HenvendelseMottattHendelse>()
            else -> {
                logger.error { "rehydrerTilstandsendringHendelse: Ukjent hendelse med type $hendelseType" }
                sikkerlogger.error { "rehydrerTilstandsendringHendelse: Ukjent hendelse med type $hendelseType: $hendelseJson" }
                throw IllegalArgumentException("Ukjent hendelse type $hendelseType")
            }
        }
    }

    private fun TransactionalSession.lagre(henvendelse: Henvendelse) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO henvendelse_v1
                        (id, person_id, journalpost_id, skjema_kode, mottatt, behandler_ident, tilstand)
                    VALUES
                        (:id, :person_id, :journalpost_id, : skjema_kode, :mottatt, :behandler_ident, :tilstand)
                    ON CONFLICT (id) DO UPDATE SET skjermes_som_egne_ansatte = :skjermes_som_egne_ansatte , adressebeskyttelse_gradering = :adressebeskyttelse_gradering             
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to henvendelse.henvendelseId,
                        "person_id" to henvendelse.person.id,
                        "journalpost_id" to henvendelse.journalpostId,
                        "skjema_kode" to henvendelse.skjemaKode,
                        "mottatt" to henvendelse.mottatt,
                        "behandler_ident" to henvendelse.behandlerIdent(),
                        "tilstand" to henvendelse.tilstand().type.name,
                    ),
            ).asUpdate,
        )
    }
}
