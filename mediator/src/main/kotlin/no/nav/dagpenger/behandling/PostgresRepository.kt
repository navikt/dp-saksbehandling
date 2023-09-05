package no.nav.dagpenger.behandling

import kotliquery.Row
import kotliquery.Session
import kotliquery.action.UpdateQueryAction
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.oppgave.OppgaveRepository
import java.time.LocalDate
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

    private inline fun <reified T> Row.hentSvar(extractFun: () -> T): Svar<T> {
        val verdi = when {
            this.boolean("ubesvart") -> {
                null
            }

            else -> {
                extractFun()
            }
        }
        return Svar(verdi, T::class.java, NullSporing())
    }

    private fun Session.hentSteg(behandlingId: UUID): Set<Steg<*>> {
        val run = this.run(
            queryOf(
                //language=PostgreSQL
                statement = """    
                    SELECT uuid, steg_id, tilstand, type, svar_type, ubesvart, string, dato, heltall, boolsk, desimal
                    FROM steg WHERE behandling_uuid = :behandling_uuid
                """.trimIndent(),
                paramMap = mapOf("behandling_uuid" to behandlingId),

            ).map { row ->
                val type = row.string("type")
                val stegId = row.string("steg_id")
                val stegUUID = row.uuid("uuid")
                when (type) {
                    "Vilkår" -> {
                        Steg.Vilkår(uuid = stegUUID, id = stegId)
                    }

                    "Fastsettelse" -> {
                        when (val svarType = row.string("svar_type")) {
                            "LocalDate" -> {
                                Steg.Fastsettelse.rehydrer<LocalDate>(
                                    stegUUID,
                                    stegId,
                                    row.hentSvar {
                                        row.localDate("dato")
                                    },
                                )
                            }

                            "Integer" -> {
                                Steg.Fastsettelse.rehydrer<Int>(
                                    stegUUID,
                                    stegId,
                                    row.hentSvar {
                                        row.int("heltall")
                                    },
                                )
                            }

                            "String" -> {
                                Steg.Fastsettelse.rehydrer<String>(
                                    stegUUID,
                                    stegId,
                                    row.hentSvar {
                                        row.string("string")
                                    },
                                )
                            }

                            "Boolean" -> {
                                Steg.Fastsettelse.rehydrer<Boolean>(
                                    stegUUID,
                                    stegId,
                                    row.hentSvar {
                                        row.boolean("boolsk")
                                    },
                                )
                            }

                            "Double" -> {
                                Steg.Fastsettelse.rehydrer<Double>(
                                    stegUUID,
                                    stegId,
                                    row.hentSvar {
                                        row.double("desimal")
                                    },
                                )
                            }

                            else -> throw IllegalArgumentException("Ugyldig svartype: $svarType")
                        }
                    }

                    else -> throw IllegalArgumentException("Ugyldig type: $type")
                }
            }.asList,
        )

        return run.toSet().also { this.leggTilRelasjoner(behandlingId, it) }
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
        TODO("Not yet implemented")
    }

    override fun hentOppgave(uuid: UUID): Oppgave {
        TODO("Not yet implemented")
    }

    override fun hentOppgaver(): List<Oppgave> {
        TODO("Not yet implemented")
    }

    override fun hentOppgaverFor(fnr: String): List<Oppgave> {
        TODO("Not yet implemented")
    }

    private fun behandlingInsertStatementBuilder(behandling: Behandling): List<UpdateQueryAction> {
        val s1 = queryOf(
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

        val s2 = behandling.alleSteg().map { steg ->
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

        val s3 = behandling.alleSteg().flatMap { parent ->
            parent.avhengigeSteg().map { children -> Pair(parent, children) }
        }.toSet().map {
            queryOf(
                //language=PostgreSQL
                statement = """
                   INSERT INTO steg_relasjon(behandling_id, parent_id, child_id) VALUES (:behandling_id, :parent_id, :child_id)
                   ON CONFLICT (behandling_id,parent_id,child_id) DO NOTHING 
                """.trimIndent(),
                paramMap = mapOf(
                    "behandling_id" to behandling.uuid,
                    "parent_id" to it.first.uuid,
                    "child_id" to it.second.uuid,
                ),
            ).asUpdate
        }

        return listOf(s1) + s2 + s3
    }
}

private fun Steg<*>.toParamMap(): Map<String, Any?> {
    val svarType = svar.clazz.simpleName

    return mapOf(
        "uuid" to uuid,
        "steg_id" to id,
        "tilstand" to tilstand.toString(),
        "type" to this.javaClass.simpleName,
        "svar_type" to svarType,
        "ubesvart" to svar.ubesvart,
        "string" to svar.verdi.takeIf {
            it != null && svarType == "String"
        },
        "dato" to svar.verdi.takeIf {
            it != null && svarType == "LocalDate"
        },
        "heltall" to svar.verdi.takeIf {
            it != null && svarType == "Integer"
        },
        "boolsk" to svar.verdi.takeIf {
            it != null && svarType == "Boolean"
        },
        "desimal" to svar.verdi.takeIf {
            it != null && svarType == "Double"
        },
    )
}

class LagrePersonStatementBuilder(private val person: Person) : PersonVisitor {
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
