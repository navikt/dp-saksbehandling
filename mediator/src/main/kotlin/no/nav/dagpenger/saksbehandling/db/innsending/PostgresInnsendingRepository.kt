package no.nav.dagpenger.saksbehandling.db.innsending

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.innsending.Innsending
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.Type
import no.nav.dagpenger.saksbehandling.innsending.InnsendingTilstandslogg
import no.nav.dagpenger.saksbehandling.serder.tilHendelse
import no.nav.dagpenger.saksbehandling.serder.tilJson
import org.postgresql.util.PGobject
import java.util.UUID
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class PostgresInnsendingRepository(private val dataSource: DataSource) : InnsendingRepository {
    override fun lagre(innsending: Innsending) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagre(innsending)
            }
        }
    }

    override fun hent(innsendingId: UUID): Innsending {
        return finnInnsending(innsendingId)
            ?: throw DataNotFoundException("Kan ikke finne innsending med id $innsendingId")
    }

    override fun finnInnsendingerForPerson(ident: String): List<Innsending> {
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  inns.id as innsending_id, 
                                inns.journalpost_id, 
                                inns.skjema_kode, 
                                inns.kategori,
                                inns.mottatt, 
                                inns.behandler_ident, 
                                inns.tilstand,
                                pers.id as person_id, 
                                pers.ident, 
                                pers.skjermes_som_egne_ansatte, 
                                pers.adressebeskyttelse_gradering as adressebeskyttelse
                        FROM    innsending_v1 inns
                        JOIN    person_v1 pers ON pers.id = inns.person_id
                        WHERE   pers.ident = :ident
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                        ),
                ).map { row ->
                    val innsendingId = row.uuid("innsending_id")
                    val tilstandslogg = hentTilstandsloggForInnsending(innsendingId)
                    row.rehydrerInnsending(tilstandslogg)
                }.asList,
            )
        }
    }

    private fun TransactionalSession.lagre(
        innsendingId: UUID,
        tilstandsendring: Tilstandsendring<Type>,
    ) {
        this.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO innsending_tilstand_logg_v1
                        (id, innsending_id, tilstand, hendelse_type, hendelse, tidspunkt)
                    VALUES
                        (:id, :innsending_id, :tilstand,:hendelse_type, :hendelse, :tidspunkt)
                    ON CONFLICT DO NOTHING
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to tilstandsendring.id,
                        "innsending_id" to innsendingId,
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

    private fun finnInnsending(innsendingId: UUID): Innsending? {
        val tilstandLoggg = hentTilstandsloggForInnsending(innsendingId)
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  inns.id as innsending_id, 
                                inns.journalpost_id, 
                                inns.skjema_kode, 
                                inns.kategori,
                                inns.mottatt, 
                                inns.behandler_ident, 
                                inns.tilstand,
                                pers.id as person_id, 
                                pers.ident, 
                                pers.skjermes_som_egne_ansatte, 
                                pers.adressebeskyttelse_gradering as adressebeskyttelse
                        FROM    innsending_v1 inns
                        JOIN    person_v1 pers ON pers.id = inns.person_id
                        WHERE   inns.id = :innsending_id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "innsending_id" to innsendingId,
                        ),
                ).map { row ->
                    row.rehydrerInnsending(tilstandLoggg)
                }.asSingle,
            )
        }
    }

    private fun Row.rehydrerInnsending(tilstandsloggg: InnsendingTilstandslogg): Innsending {
        val innsendingId = this.uuid("innsending_id")
        val tilstand =
            runCatching {
                when (Type.valueOf(value = string("tilstand"))) {
                    Type.KLAR_TIL_BEHANDLING -> Innsending.Tilstand.KlarTilBehandling
                    Type.UNDER_BEHANDLING -> Innsending.Tilstand.UnderBehandling
                    Type.FERDIGBEHANDLET -> Innsending.Tilstand.Ferdigbehandlet
                    Type.AVBRUTT -> Innsending.Tilstand.Avbrutt
                }
            }.getOrElse { t ->
                throw RuntimeException("Kunne ikke rehydrere innsending til tilstand: ${string("tilstand")} ${t.message}")
            }

        return Innsending.rehydrer(
            innsendingId = innsendingId,
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

    private fun hentTilstandsloggForInnsending(innsendingId: UUID): InnsendingTilstandslogg {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT id, innsending_id, tilstand, hendelse_type, hendelse, tidspunkt
                        FROM   innsending_tilstand_logg_v1
                        WHERE  innsending_id = :innsending_id
                        """.trimIndent(),
                    paramMap = mapOf("innsending_id" to innsendingId),
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
            ).let { InnsendingTilstandslogg(it) }
        }
    }

    private fun rehydrerTilstandsendringHendelse(
        hendelseType: String,
        hendelseJson: String,
    ): Hendelse {
        return when (hendelseType) {
            "InnsendingMottattHendelse" -> hendelseJson.tilHendelse<InnsendingMottattHendelse>()
            "TildelHendelse" -> hendelseJson.tilHendelse<TildelHendelse>()
            "FjernAnsvarHendelse" -> hendelseJson.tilHendelse<FjernAnsvarHendelse>()
            "InnsendingFerdigstiltHendelse" -> hendelseJson.tilHendelse<InnsendingFerdigstiltHendelse>()
            "BehandlingOpprettetForSøknadHendelse" -> hendelseJson.tilHendelse<BehandlingOpprettetForSøknadHendelse>()
            else -> {
                logger.error { "rehydrerTilstandsendringHendelse: Ukjent hendelse med type $hendelseType" }
                sikkerlogger.error { "rehydrerTilstandsendringHendelse: Ukjent hendelse med type $hendelseType: $hendelseJson" }
                throw IllegalArgumentException("Ukjent hendelse type $hendelseType")
            }
        }
    }

    private fun TransactionalSession.lagre(innsending: Innsending) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO innsending_v1
                        (id, person_id, journalpost_id, skjema_kode, kategori, mottatt, behandler_ident, tilstand)
                    VALUES
                        (:id, :person_id, :journalpost_id, :skjema_kode, :kategori, :mottatt, :behandler_ident, :tilstand)
                    ON CONFLICT (id) DO UPDATE SET behandler_ident = :behandler_ident, tilstand = :tilstand     
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to innsending.innsendingId,
                        "person_id" to innsending.person.id,
                        "journalpost_id" to innsending.journalpostId,
                        "skjema_kode" to innsending.skjemaKode,
                        "kategori" to innsending.kategori.name,
                        "mottatt" to innsending.mottatt,
                        "behandler_ident" to innsending.behandlerIdent(),
                        "tilstand" to innsending.tilstand().type.name,
                    ),
            ).asUpdate,
        )
        innsending.tilstandslogg.forEach { tilstandsendring ->
            lagre(innsending.innsendingId, tilstandsendring)
        }
    }
}
