package no.nav.dagpenger.behandling

import kotliquery.Session
import kotliquery.action.UpdateQueryAction
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.oppgave.OppgaveRepository
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PostgresRepository(private val ds: DataSource) : PersonRepository, OppgaveRepository {

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

    private fun Session.hentSteg(behandlingId: UUID): Set<Steg<*>> {
        val run = this.run(
            queryOf(
                //language=PostgreSQL
                statement = """    
                SELECT uuid, steg_id, tilstand, type, svar_type, ubesvart, string, dato, heltall, boolsk, desimal
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

                val svar = when (val svarType = row.string("svar_type")) {
                    "LocalDateSvar" -> Svar.LocalDateSvar(row.localDateOrNull("dato"), sporing)
                    "IntegerSvar" -> Svar.IntegerSvar(row.intOrNull("heltall"), sporing)
                    "StringSvar" -> Svar.StringSvar(row.stringOrNull("string"), sporing)
                    "BooleanSvar" -> Svar.BooleanSvar(row.boolean("boolsk"), sporing)
                    "DoubleSvar" -> Svar.DoubleSvar(row.doubleOrNull("desimal"), sporing)
                    else -> throw IllegalArgumentException("Ugyldig svartype: $svarType")
                }
                when (type) {
                    "Vilkår" -> Steg.Vilkår.rehydrer(stegUUID, stegId, svar as Svar<Boolean>, tilstand)
                    "Prosess" -> Steg.Prosess.rehydrer(stegUUID, stegId, svar as Svar<Boolean>, tilstand)
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
                    "ManuellSporing" -> ManuellSporing(
                        utført,
                        Saksbehandler(row.string("utført_av")),
                        row.string("begrunnelse"),
                    )

                    "QuizSporing" -> QuizSporing(
                        utført,
                        row.string("json"),
                    )

                    else -> throw IllegalStateException("Kjenner ikke til type=$type")
                }
            }.asSingle,
        ) ?: NullSporing
    }

    private fun Session.leggTilRelasjoner(behandlingId: UUID, steg: Set<Steg<*>>) {
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

    internal fun hentBehandling(uuid: UUID): Behandling {
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
                        tilstand = row.string("tilstand"),
                        behandler = listOf(), // todo implement
                        // todo: Sak er allerede rehydrert som en del av person opphentingen...
                        sak = Sak(row.uuid("sak_id")),
                    )
                }.asSingle,
            ) ?: throw NotFoundException("Behandling ikke funnet: $uuid")
        }
    }

    internal class NotFoundException(msg: String) : RuntimeException(msg)

    internal fun lagreBehandling(behandling: Behandling) {
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                behandlingInsertStatementBuilder(behandling).forEach {
                    tx.run(it)
                }
            }
        }
    }

    override fun lagreOppgave(oppgave: Oppgave) {
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                OppgaveStmtBuilder(oppgave).queries.forEach {
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
                        uuid,
                        behandling,
                        row.stringOrNull("utføres_av"),
                        row.localDateTime("opprettet"),
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
                    Oppgave.rehydrer(
                        row.uuid("uuid"),
                        behandling,
                        row.stringOrNull("utføres_av"),
                        row.localDateTime("opprettet"),
                    )
                }.asList,
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
                    Oppgave.rehydrer(
                        row.uuid("uuid"),
                        behandling,
                        row.stringOrNull("utføres_av"),
                        row.localDateTime("opprettet"),
                    )
                }.asList,
            )
        }
    }

    private fun behandlingInsertStatementBuilder(behandling: Behandling): List<UpdateQueryAction> {
        val qBehandling = queryOf(
            //language=PostgreSQL
            statement = """
               INSERT INTO behandling(person_ident, opprettet, uuid, tilstand, sak_id)
               VALUES (:person_ident, :opprettet, :uuid, :tilstand, :sak_id)
               ON CONFLICT(uuid) DO UPDATE SET tilstand = :tilstand
            """.trimIndent(),
            paramMap = mapOf(
                "person_ident" to behandling.person.ident,
                "opprettet" to behandling.opprettet,
                "uuid" to behandling.uuid,
                "tilstand" to behandling.tilstand.javaClass.simpleName,
                "sak_id" to behandling.sak.id,
            ),
        ).asUpdate

        val qSteg = behandling.alleSteg().map { steg ->
            queryOf(
                //language=PostgreSQL
                statement = """
                INSERT INTO steg (behandling_uuid, uuid, steg_id, tilstand, type, svar_type, ubesvart, string, dato, heltall, boolsk, desimal)
                VALUES (:behandling_uuid, :uuid, :steg_id, :tilstand, :type, :svar_type, :ubesvart, :string, :dato, :heltall, :boolsk, :desimal)
                ON CONFLICT(uuid) DO UPDATE SET tilstand = :tilstand, ubesvart = :ubesvart, string = :string, dato = :dato, heltall = :heltall , boolsk = :boolsk, desimal = :desimal
                """.trimIndent(),
                paramMap = mapOf("behandling_uuid" to behandling.uuid) + steg.toParamMap(),
            ).asUpdate
        }

        val qRelasjoner = behandling.alleSteg().flatMap { parent ->
            parent.avhengigeSteg().map { children -> Pair(parent, children) }
        }.toSet().map {
            queryOf(
                //language=PostgreSQL
                statement = """
                INSERT INTO steg_relasjon(behandling_id, parent_id, child_id)
                VALUES (:behandling_id, :parent_id, :child_id)
                ON CONFLICT (behandling_id,parent_id,child_id) DO NOTHING 
                """.trimIndent(),
                paramMap = mapOf(
                    "behandling_id" to behandling.uuid,
                    "parent_id" to it.first.uuid,
                    "child_id" to it.second.uuid,
                ),
            ).asUpdate
        }
        val qSporing = behandling.alleSteg().filter { it.svar.sporing !is NullSporing }.map { steg ->
            val sporing = steg.svar.sporing
            queryOf(
                //language=PostgreSQL
                statement = """
                INSERT INTO sporing(steg_uuid, utført, begrunnelse, utført_av, json, type)
                VALUES (:steg_uuid, :utfort, :begrunnelse, :utfort_av, :json, :type)
                ON CONFLICT(steg_uuid ) DO UPDATE SET utført      = :utfort,
                                                      begrunnelse = :begrunnelse,
                                                      utført_av   = :utfort_av,
                                                      json        = :json,
                                                      type        = :type
                """.trimIndent(),
                paramMap = mapOf(
                    "steg_uuid" to steg.uuid,
                ) + sporing.toParamMap(),
            ).asUpdate
        }

        return listOf(qBehandling) + qSteg + qRelasjoner + qSporing
    }

    private inner class OppgaveStmtBuilder(oppgave: Oppgave) : OppgaveVisitor {
        private lateinit var oppgaveId: UUID
        private lateinit var opprettet: LocalDateTime
        private var utføresAv: Saksbehandler? = null
        val queries = mutableListOf<UpdateQueryAction>()

        init {
            oppgave.accept(this)
        }

        override fun visit(oppgaveId: UUID, opprettet: LocalDateTime, utføresAv: Saksbehandler?) {
            this.oppgaveId = oppgaveId
            this.opprettet = opprettet
            this.utføresAv = utføresAv
        }

        override fun visit(behandling: Behandling) {
            queries.addAll(behandlingInsertStatementBuilder(behandling))
            queries.add(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO oppgave (uuid, opprettet,utføres_av, behandling_id) 
                    VALUES (:uuid, :opprettet, :utfores_av, :behandling_id)
                    ON CONFLICT (uuid) DO UPDATE SET utføres_av = :utfores_av
                    """.trimIndent(),
                    mapOf(
                        "uuid" to oppgaveId,
                        "opprettet" to opprettet,
                        "utfores_av" to utføresAv?.ident,
                        "behandling_id" to behandling.uuid,
                    ),
                ).asUpdate,
            )
        }
    }
}

private fun Sporing.toParamMap(): Map<String, Any?> {
    val (utførtAv, begrunnelse) = when (this is ManuellSporing) {
        true -> Pair(this.utførtAv.ident, this.begrunnelse)
        else -> Pair(null, null)
    }

    val json = when (this is QuizSporing) {
        true -> PGobject().also {
            it.type = "JSONB"
            it.value = this.json
        }

        false -> null
    }

    return mapOf(
        "utfort" to this.utført,
        "begrunnelse" to begrunnelse,
        "utfort_av" to utførtAv,
        "json" to json,
        "type" to this.javaClass.simpleName,
    )
}

private fun Steg<*>.toParamMap(): Map<String, Any?> {
    val svarType = svar

    return mapOf(
        "uuid" to uuid,
        "steg_id" to id,
        "tilstand" to tilstand.toString(),
        "type" to this.javaClass.simpleName,
        "svar_type" to svarType.javaClass.simpleName,
        "ubesvart" to svar.ubesvart,
        "string" to svar.verdi.takeIf {
            it != null && svarType is Svar.StringSvar
        },
        "dato" to svar.verdi.takeIf {
            it != null && svarType is Svar.LocalDateSvar
        },
        "heltall" to svar.verdi.takeIf {
            it != null && svarType is Svar.IntegerSvar
        },
        "boolsk" to svar.verdi.takeIf {
            it != null && svarType is Svar.BooleanSvar
        },
        "desimal" to svar.verdi.takeIf {
            it != null && svarType is Svar.DoubleSvar
        },
    )
}

private class LagrePersonStatementBuilder(private val person: Person) : PersonVisitor {
    val queries = mutableListOf<UpdateQueryAction>().also {
        it.add(
            queryOf(
                //language=PostgreSQL
                statement = """INSERT INTO person(ident) VALUES (:ident) ON CONFLICT DO NOTHING""",
                paramMap = mapOf(
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
                    paramMap = mapOf(
                        "uuid" to sak.id,
                        "person_ident" to person.ident,
                    ),
                ).asUpdate,
            )
        }
    }
}
