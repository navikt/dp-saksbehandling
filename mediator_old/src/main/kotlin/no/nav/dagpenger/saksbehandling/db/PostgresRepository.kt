package no.nav.dagpenger.saksbehandling.db

import kotliquery.Session
import kotliquery.action.UpdateQueryAction
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.ManuellSporing
import no.nav.dagpenger.saksbehandling.NullSporing
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.PersonVisitor
import no.nav.dagpenger.saksbehandling.QuizSporing
import no.nav.dagpenger.saksbehandling.Rolle
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.Sporing
import no.nav.dagpenger.saksbehandling.Steg
import no.nav.dagpenger.saksbehandling.Svar
import no.nav.dagpenger.saksbehandling.Tilstand
import no.nav.dagpenger.saksbehandling.hendelser.PersonHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakStansetHendelse
import no.nav.dagpenger.saksbehandling.oppgave.Oppgave
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class PostgresRepository(private val ds: DataSource) : PersonRepository, OppgaveRepository, BehandlingRepository {
    override fun hentPerson(ident: String): Person? =
        using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """SELECT ident FROM person WHERE ident = :ident""",
                    paramMap = mapOf("ident" to ident),
                ).map { row ->
                    val funnetIdent = row.string("ident")
                    Person.rehydrer(funnetIdent, hentSakerFor(funnetIdent))
                }.asSingle,
            )
        }

    override fun lagrePerson(person: Person): Unit =
        using(sessionOf(ds)) { session ->
            val queries = LagrePersonStatementBuilder(person).queries
            session.transaction { tx ->
                queries.forEach {
                    tx.run(it)
                }
            }
        }

    private fun hentSakerFor(ident: String): Set<Sak> {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """SELECT uuid FROM sak WHERE person_ident = :person_ident""",
                    paramMap = mapOf("person_ident" to ident),
                ).map { row ->
                    Sak(
                        row.uuid("uuid"),
                    )
                }.asList,
            )
        }.toSet()
    }

    private fun Session.hentBehandler(
        behandlingId: UUID,
        ident: String,
    ): List<PersonHendelse> {
        return this.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    SELECT melding_referanse_id, clazz, soknad_id, journalpost_id, oppgave_id, soknad_innsendt_dato
                    FROM  hendelse
                    WHERE behandling_id = :behandling_id
                    """.trimIndent(),
                paramMap = mapOf("behandling_id" to behandlingId),
            ).map { row ->
                val clazz = row.string("clazz")
                val meldingsreferanseId = row.uuid("melding_referanse_id")
                when (clazz) {
                    "SøknadInnsendtHendelse" -> {
                        SøknadInnsendtHendelse(
                            meldingsreferanseId = meldingsreferanseId,
                            søknadId = row.uuid("soknad_id"),
                            journalpostId = row.string("journalpost_id"),
                            ident = ident,
                            innsendtDato = row.localDateOrNull("soknad_innsendt_dato") ?: LocalDate.MIN,
                        )
                    }

                    "VedtakStansetHendelse" -> {
                        VedtakStansetHendelse(
                            meldingsreferanseId = meldingsreferanseId,
                            ident = ident,
                            oppgaveId = row.uuid("oppgave_id"),
                        )
                    }

                    else -> {
                        throw IllegalArgumentException("Ugyldig clazz: $clazz")
                    }
                }
            }.asList,
        )
    }

    private fun Session.hentSteg(behandlingId: UUID): Set<Steg<*>> {
        val run =
            this.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """    
                        SELECT uuid, steg_id, tilstand, type, svar_type, ubesvart, string, dato, heltall, boolsk, desimal, rolle, krever_totrinnskontroll
                        FROM steg WHERE behandling_uuid = :behandling_uuid
                        ORDER BY id
                        """.trimIndent(),
                    paramMap = mapOf("behandling_uuid" to behandlingId),
                ).map { row ->
                    val type = row.string("type")
                    val stegId = row.string("steg_id")
                    val stegUUID = row.uuid("uuid")
                    val sporing = hentSporing(stegUUID)
                    val tilstand = Tilstand.valueOf(row.string("tilstand"))
                    val rolle = row.stringOrNull("rolle")?.let { Rolle.valueOf(it) }
                    val kreverTotrinnskontroll = row.boolean("krever_totrinnskontroll")

                    val svar =
                        when (val svarType = row.string("svar_type")) {
                            "LocalDateSvar" -> Svar.LocalDateSvar(row.localDateOrNull("dato"), sporing)
                            "IntegerSvar" -> Svar.IntegerSvar(row.intOrNull("heltall"), sporing)
                            "StringSvar" -> Svar.StringSvar(row.stringOrNull("string"), sporing)
                            "BooleanSvar" -> Svar.BooleanSvar(row.boolean("boolsk"), sporing)
                            "DoubleSvar" -> Svar.DoubleSvar(row.doubleOrNull("desimal"), sporing)
                            else -> throw IllegalArgumentException("Ugyldig svartype: $svarType")
                        }
                    when (type) {
                        "Vilkår" -> Steg.Vilkår.rehydrer(stegUUID, stegId, svar as Svar<Boolean>, tilstand)
                        "Prosess" ->
                            Steg.Prosess.rehydrer(
                                stegUUID,
                                stegId,
                                svar as Svar<Boolean>,
                                tilstand,
                                rolle ?: throw IllegalStateException("Forventet at rolle er satt for prosess-steg"),
                                kreverTotrinnskontroll,
                            )

                        "Fastsettelse" -> Steg.Fastsettelse.rehydrer(stegUUID, stegId, svar, tilstand)
                        else -> throw IllegalArgumentException("Ugyldig type: $type")
                    }
                }.asList,
            )

        return run.toSet().also { this.leggTilRelasjoner(behandlingId, it) }
    }

    private fun Session.hentSporing(stegUUID: UUID): Sporing {
        return run(
            queryOf(
                //language=PostgreSQL
                """
                SELECT * FROM sporing WHERE steg_uuid = :uuid
                """.trimIndent(),
                mapOf("uuid" to stegUUID),
            ).map { row ->
                val utført = row.localDateTime("utført")
                when (val type = row.string("type")) {
                    "ManuellSporing" ->
                        ManuellSporing(
                            utført,
                            Saksbehandler(row.string("utført_av")),
                            row.string("begrunnelse"),
                        )

                    "QuizSporing" ->
                        QuizSporing(
                            utført,
                            row.string("json"),
                        )

                    else -> throw IllegalStateException("Kjenner ikke til type=$type")
                }
            }.asSingle,
        ) ?: NullSporing
    }

    private fun Session.leggTilRelasjoner(
        behandlingId: UUID,
        steg: Set<Steg<*>>,
    ) {
        this.run(
            queryOf(
                //language=PostgreSQL
                statement = """SELECT parent_id, child_id FROM steg_relasjon WHERE behandling_id = :behandling_id""",
                paramMap = mapOf("behandling_id" to behandlingId),
            ).map { row ->
                Pair(row.uuid("parent_id"), row.uuid("child_id"))
            }.asList,
        ).toSet().forEach { steg.leggTilRelasjon(it) }
    }

    private fun Set<Steg<*>>.leggTilRelasjon(relasjon: Pair<UUID, UUID>) {
        this.getByUUID(relasjon.first).avhengerAv(this.getByUUID(relasjon.second))
    }

    private fun Set<Steg<*>>.getByUUID(uuid: UUID): Steg<*> {
        return this.first { it.uuid == uuid }
    }

    override fun hentBehandling(uuid: UUID): Behandling {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """SELECT id, person_ident, opprettet, uuid, tilstand, sak_id FROM behandling WHERE uuid= :uuid""",
                    paramMap = mapOf("uuid" to uuid),
                ).map { row ->
                    val person =
                        hentPerson(row.string("person_ident")) ?: throw NotFoundException("Person ikke funnet")
                    Behandling.rehydrer(
                        person = person,
                        steg = session.hentSteg(uuid),
                        opprettet = row.localDateTime("opprettet"),
                        uuid = row.uuid("uuid"),
                        tilstand = Behandling.TilstandType.valueOf(row.string("tilstand")),
                        behandler = session.hentBehandler(uuid, person.ident),
                        // todo: Sak er allerede rehydrert som en del av person opphentingen...
                        sak = Sak(row.uuid("sak_id")),
                    )
                }.asSingle,
            ) ?: throw NotFoundException("Behandling ikke funnet: $uuid")
        }
    }

    internal class NotFoundException(msg: String) : RuntimeException(msg)

    override fun lagreOppgave(oppgave: Oppgave) {
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                listOf(
                    // todo Order matters...
                    BehandlingStatementBuilder(oppgave).updateQueryActions,
                    HendelseStatemenBuilder(oppgave).updateQueryActions,
                    StegStatementBuilder(oppgave).updateQueryActions,
                    RelasjonStatementBuilder(oppgave).updateQueryActions,
                    SporingStatementBuilder(oppgave).updateQueryActions,
                    OppgaveStatementBuilder(oppgave).updateQueryActions,
                ).flatten().forEach {
                    tx.run(it)
                }
            }
        }
    }

    override fun hentOppgave(uuid: UUID): Oppgave {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT opprettet, utføres_av, behandling_id
                    FROM oppgave
                    WHERE uuid = :uuid
                    """.trimIndent(),
                    mapOf("uuid" to uuid),
                ).map { row ->
                    val behandling: Behandling = hentBehandling(row.uuid("behandling_id"))
                    Oppgave.rehydrer(
                        uuid = uuid,
                        behandling = behandling,
                        utføresAv = row.stringOrNull("utføres_av"),
                        opprettet = row.localDateTime("opprettet"),
                        emneknagger = session.hentEmneknaggerFor(uuid).toSet(),
                    )
                }.asSingle,
            ) ?: throw IllegalArgumentException("Fant ikke oppgave med uuid=$uuid")
        }
    }

    override fun hentOppgaver(): List<Oppgave> {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT oppgave.uuid, oppgave.opprettet, utføres_av, behandling_id FROM oppgave ORDER BY opprettet DESC
                    """.trimIndent(),
                ).map { row ->
                    val behandling: Behandling = hentBehandling(row.uuid("behandling_id"))
                    val oppgaveUUID = row.uuid("uuid")
                    Oppgave.rehydrer(
                        uuid = oppgaveUUID,
                        behandling = behandling,
                        utføresAv = row.stringOrNull("utføres_av"),
                        opprettet = row.localDateTime("opprettet"),
                        emneknagger = session.hentEmneknaggerFor(oppgaveUUID).toSet(),
                    )
                }.asList,
            )
        }
    }

    override fun hentOppgaveFor(søknadUUID: UUID): Oppgave {
        val oppgaveveId =
            hentOppgaveUUIDFor(søknadUUID) ?: throw IllegalArgumentException("Fant ikke oppgave for søknad=$søknadUUID")
        return hentOppgave(oppgaveveId)
    }

    private fun hentOppgaveUUIDFor(søknadId: UUID): UUID? {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        SELECT o.uuid
                        FROM oppgave o
                        JOIN behandling b ON b.uuid = o.behandling_id
                        JOIN hendelse h ON b.uuid = h.behandling_id
                        WHERE h.soknad_id = :soknad_id """,
                    paramMap = mapOf("soknad_id" to søknadId),
                ).map { row ->
                    row.uuid("uuid")
                }.asSingle,
            )
        }
    }

    override fun hentOppgaverFor(fnr: String): List<Oppgave> {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT oppgave.uuid, oppgave.opprettet, utføres_av, behandling_id
                    FROM oppgave
                    LEFT JOIN behandling b ON b.uuid = oppgave.behandling_id
                    WHERE b.person_ident = :fnr
                    """.trimIndent(),
                    mapOf("fnr" to fnr),
                ).map { row ->
                    val behandling: Behandling = hentBehandling(row.uuid("behandling_id"))
                    val oppgaveUUID = row.uuid("uuid")
                    Oppgave.rehydrer(
                        uuid = oppgaveUUID,
                        behandling = behandling,
                        utføresAv = row.stringOrNull("utføres_av"),
                        opprettet = row.localDateTime("opprettet"),
                        emneknagger = session.hentEmneknaggerFor(oppgaveUUID).toSet(),
                    )
                }.asList,
            )
        }
    }
}

private fun Session.hentEmneknaggerFor(oppgaveUUID: UUID) =
    this.run(
        queryOf(
            //language=PostgreSQL
            statement =
                """
                |SELECT emneknagg 
                |FROM oppgave_emneknagg
                |WHERE oppgave_uuid = :oppgave_uuid  
                """.trimMargin(),
            paramMap = mapOf("oppgave_uuid" to oppgaveUUID),
        ).map { rad ->
            rad.string("emneknagg")
        }.asList,
    )

private class LagrePersonStatementBuilder(private val person: Person) : PersonVisitor {
    val queries =
        mutableListOf<UpdateQueryAction>().also {
            it.add(
                queryOf(
                    //language=PostgreSQL
                    statement = """INSERT INTO person(ident) VALUES (:ident) ON CONFLICT DO NOTHING""",
                    paramMap =
                        mapOf(
                            "ident" to person.ident,
                        ),
                ).asUpdate,
            )
        }

    init {
        person.accept(this)
    }

    override fun visit(saker: Set<Sak>) {
        saker.forEach { sak ->
            queries.add(
                queryOf(
                    //language=PostgreSQL
                    statement = """INSERT INTO sak(uuid, person_ident) VALUES (:uuid, :person_ident) ON CONFLICT DO NOTHING""",
                    paramMap =
                        mapOf(
                            "uuid" to sak.id,
                            "person_ident" to person.ident,
                        ),
                ).asUpdate,
            )
        }
    }
}
