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
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.Type
import no.nav.dagpenger.saksbehandling.henvendelse.HenvendelseTilstandslogg
import no.nav.dagpenger.saksbehandling.serder.tilHendelse
import no.nav.dagpenger.saksbehandling.serder.tilJson
import org.postgresql.util.PGobject
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

    override fun finnHenvendelserForPerson(ident: String): List<Henvendelse> {
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  henv.id as henvendelse_id, 
                                henv.journalpost_id, 
                                henv.skjema_kode, 
                                henv.kategori,
                                henv.mottatt, 
                                henv.behandler_ident, 
                                henv.tilstand,
                                pers.id as person_id, 
                                pers.ident, 
                                pers.skjermes_som_egne_ansatte, 
                                pers.adressebeskyttelse_gradering as adressebeskyttelse
                        FROM    henvendelse_v1 henv
                        JOIN    person_v1 pers ON pers.id = henv.person_id
                        WHERE   pers.ident = :ident
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                        ),
                ).map { row ->
                    val henvendelseId = row.uuid("henvendelse_id")
                    val tilstandslogg = hentTilstandsloggForHenvendelse(henvendelseId)
                    row.rehydrerHenvendelse(tilstandslogg)
                }.asList,
            )
        }
    }

    private fun TransactionalSession.lagre(
        henvendelseId: UUID,
        tilstandsendring: Tilstandsendring<Type>,
    ) {
        this.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO henvendelse_tilstand_logg_v1
                        (id, henvendelse_id, tilstand, hendelse_type, hendelse, tidspunkt)
                    VALUES
                        (:id, :henvendelse_id, :tilstand,:hendelse_type, :hendelse, :tidspunkt)
                    ON CONFLICT DO NOTHING
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to tilstandsendring.id,
                        "henvendelse_id" to henvendelseId,
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

    private fun finnHenvendelse(henvendelseId: UUID): Henvendelse? {
        val tilstandLoggg = hentTilstandsloggForHenvendelse(henvendelseId)
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  henv.id as henvendelse_id, 
                                henv.journalpost_id, 
                                henv.skjema_kode, 
                                henv.kategori,
                                henv.mottatt, 
                                henv.behandler_ident, 
                                henv.tilstand,
                                pers.id as person_id, 
                                pers.ident, 
                                pers.skjermes_som_egne_ansatte, 
                                pers.adressebeskyttelse_gradering as adressebeskyttelse
                        FROM    henvendelse_v1 henv
                        JOIN    person_v1 pers ON pers.id = henv.person_id
                        WHERE   henv.id = :henvendelse_id
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

    private fun Row.rehydrerHenvendelse(tilstandsloggg: HenvendelseTilstandslogg): Henvendelse {
        val henvendelseId = this.uuid("henvendelse_id")
        val tilstand =
            runCatching {
                when (Type.valueOf(value = string("tilstand"))) {
                    Type.KLAR_TIL_BEHANDLING -> Henvendelse.Tilstand.KlarTilBehandling
                    Type.UNDER_BEHANDLING -> Henvendelse.Tilstand.UnderBehandling
                    Type.FERDIGBEHANDLET -> Henvendelse.Tilstand.Ferdigbehandlet
                    Type.AVBRUTT -> Henvendelse.Tilstand.Avbrutt
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
                    skjermesSomEgneAnsatte = this.boolean("skjermes_som_egne_ansatte"),
                    adressebeskyttelseGradering = AdressebeskyttelseGradering.valueOf(this.string("adressebeskyttelse")),
                ),
            journalpostId = this.string("journalpost_id"),
            mottatt = this.localDateTime("mottatt"),
            skjemaKode = this.string("skjema_kode"),
            kategori = Kategori.valueOf(this.string("kategori")),
            behandlerIdent = this.stringOrNull("behandler_ident"),
            tilstand = tilstand,
            tilstandslogg = tilstandsloggg,
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
            "TildelHendelse" -> hendelseJson.tilHendelse<TildelHendelse>()
            "FjernAnsvarHendelse" -> hendelseJson.tilHendelse<FjernAnsvarHendelse>()
            "HenvendelseFerdigstiltHendelse" -> hendelseJson.tilHendelse<HenvendelseFerdigstiltHendelse>()
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
                        (id, person_id, journalpost_id, skjema_kode, kategori, mottatt, behandler_ident, tilstand)
                    VALUES
                        (:id, :person_id, :journalpost_id, :skjema_kode, :kategori, :mottatt, :behandler_ident, :tilstand)
                    ON CONFLICT (id) DO UPDATE SET behandler_ident = :behandler_ident, tilstand = :tilstand     
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to henvendelse.henvendelseId,
                        "person_id" to henvendelse.person.id,
                        "journalpost_id" to henvendelse.journalpostId,
                        "skjema_kode" to henvendelse.skjemaKode,
                        "kategori" to henvendelse.kategori.name,
                        "mottatt" to henvendelse.mottatt,
                        "behandler_ident" to henvendelse.behandlerIdent(),
                        "tilstand" to henvendelse.tilstand().type.name,
                    ),
            ).asUpdate,
        )
        henvendelse.tilstandslogg.forEach { tilstandsendring ->
            lagre(henvendelse.henvendelseId, tilstandsendring)
        }
    }
}
