package no.nav.dagpenger.behandling.db

import kotliquery.action.UpdateQueryAction
import kotliquery.queryOf
import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.ManuellSporing
import no.nav.dagpenger.behandling.NullSporing
import no.nav.dagpenger.behandling.OppgaveVisitor
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.QuizSporing
import no.nav.dagpenger.behandling.Sak
import no.nav.dagpenger.behandling.Saksbehandler
import no.nav.dagpenger.behandling.Sporing
import no.nav.dagpenger.behandling.Steg
import no.nav.dagpenger.behandling.Svar
import no.nav.dagpenger.behandling.hendelser.PersonHendelse
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.hendelser.VedtakStansetHendelse
import no.nav.dagpenger.behandling.oppgave.Oppgave
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import java.util.UUID

interface StatementBuilder {
    val updateQueryActions: List<UpdateQueryAction>
}

internal class OppaveStatementBuilder(oppgave: Oppgave) : OppgaveVisitor, StatementBuilder {
    private lateinit var oppgaveId: UUID
    private lateinit var opprettet: LocalDateTime
    private var utføresAv: Saksbehandler? = null
    private lateinit var behandlingId: UUID

    init {
        oppgave.accept(this)
    }

    override fun visit(
        oppgaveId: UUID,
        opprettet: LocalDateTime,
        utføresAv: Saksbehandler?,
    ) {
        this.oppgaveId = oppgaveId
        this.opprettet = opprettet
        this.utføresAv = utføresAv
    }

    override fun visit(behandling: Behandling) {
        this.behandlingId = behandling.uuid
    }

    override val updateQueryActions: List<UpdateQueryAction> =
        listOf(
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
                    "behandling_id" to behandlingId,
                ),
            ).asUpdate,
        )
}

internal class BehandlingStatementBuilder(oppgave: Oppgave) : OppgaveVisitor, StatementBuilder {
    lateinit var behandling: Behandling

    init {
        oppgave.accept(this)
    }

    override fun visit(behandling: Behandling) {
        this.behandling = behandling
    }

    override val updateQueryActions: List<UpdateQueryAction> =
        listOf(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO behandling(person_ident, opprettet, uuid, tilstand, sak_id)
                    VALUES (:person_ident, :opprettet, :uuid, :tilstand, :sak_id)
                    ON CONFLICT(uuid) DO UPDATE SET tilstand = :tilstand
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "person_ident" to behandling.person.ident,
                        "opprettet" to behandling.opprettet,
                        "uuid" to behandling.uuid,
                        "tilstand" to behandling.tilstand.type.toString(),
                        "sak_id" to behandling.sak.id,
                    ),
            ).asUpdate,
        )
}

internal class HendelseStatemenBuilder(oppgave: Oppgave) : OppgaveVisitor, StatementBuilder {
    private val hendelser: MutableList<PersonHendelse> = mutableListOf()
    private lateinit var behandlingId: UUID

    init {
        oppgave.accept(this)
    }

    override fun visit(
        person: Person,
        steg: Set<Steg<*>>,
        opprettet: LocalDateTime,
        behandlingId: UUID,
        tilstand: Behandling.TilstandType,
        behandler: List<PersonHendelse>,
        sak: Sak,
    ) {
        this.hendelser.addAll(behandler)
        this.behandlingId = behandlingId
    }

    override val updateQueryActions: List<UpdateQueryAction> =
        hendelser.map { hendelse ->
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO hendelse(behandling_id, melding_referanse_id, clazz, soknad_id, journalpost_id, oppgave_id, soknad_innsendt_dato)
                    VALUES (:behandling_id, :melding_referanse_id, :clazz, :soknad_id, :journalpost_id, :oppgave_id, :soknad_innsendt_dato)
                    ON CONFLICT (melding_referanse_id) DO NOTHING
                    """.trimIndent(),
                paramMap = hendelse.toParamMap(),
            ).asUpdate
        }

    private fun PersonHendelse.toParamMap(): Map<String, Any?> {
        val clazz = this.javaClass.simpleName
        return mapOf(
            "behandling_id" to behandlingId,
            "melding_referanse_id" to meldingsreferanseId(),
            "clazz" to clazz,
        ) + this.søknadInnsendtParamMap() + this.vedtakStansetParamMap()
    }

    private fun PersonHendelse.vedtakStansetParamMap(): Map<String, Any?> =
        when (this.javaClass.simpleName) {
            "VedtakStansetHendelse" -> {
                mapOf(
                    "oppgave_id" to (this as VedtakStansetHendelse).oppgaveId,
                )
            }

            else -> emptyMap()
        }

    private fun PersonHendelse.søknadInnsendtParamMap(): Map<String, Any?> =
        when (this.javaClass.simpleName) {
            "SøknadInnsendtHendelse" -> {
                val hendelse = this as SøknadInnsendtHendelse
                mapOf(
                    "soknad_id" to hendelse.søknadId(),
                    "journalpost_id" to hendelse.journalpostId(),
                    "soknad_innsendt_dato" to hendelse.innsendtDato,
                )
            }

            else -> emptyMap()
        }
}

internal class StegStatementBuilder(oppgave: Oppgave) : OppgaveVisitor, StatementBuilder {
    private lateinit var _updateQueryActions: List<UpdateQueryAction>

    init {
        oppgave.accept(this)
    }

    override fun visit(behandling: Behandling) {
        _updateQueryActions =
            behandling.alleSteg().toList().map {
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO steg (behandling_uuid, uuid, steg_id, tilstand, type, svar_type, ubesvart, string, dato, heltall, boolsk,
                                          desimal, rolle, krever_totrinnskontroll)
                        VALUES (:behandling_uuid, :uuid, :steg_id, :tilstand, :type, :svar_type, :ubesvart, :string, :dato, :heltall, :boolsk,
                                :desimal, :rolle, :krever_totrinnskontroll)
                        ON CONFLICT(uuid) DO UPDATE SET tilstand = :tilstand,
                                                        ubesvart = :ubesvart,
                                                        string   = :string,
                                                        dato     = :dato,
                                                        heltall  = :heltall,
                                                        boolsk   = :boolsk,
                                                        desimal  = :desimal,
                                                        rolle    = :rolle,
                                                        krever_totrinnskontroll = :krever_totrinnskontroll
                        """.trimIndent(),
                    paramMap = mapOf("behandling_uuid" to behandling.uuid) + it.toParamMap(),
                ).asUpdate
            }
    }

    override val updateQueryActions: List<UpdateQueryAction> = _updateQueryActions

    private fun Steg<*>.toParamMap(): Map<String, Any?> {
        val svarType = this.svar

        val rolle =
            when (this) {
                is Steg.Prosess -> rolle
                else -> null
            }

        val kreverTotrinnskontroll: Boolean =
            when (this) {
                is Steg.Prosess -> this.kreverTotrinnskontroll
                else -> false
            }

        return mapOf(
            "uuid" to uuid,
            "steg_id" to id,
            "tilstand" to tilstand.toString(),
            "type" to this.javaClass.simpleName,
            "svar_type" to svarType.javaClass.simpleName,
            "ubesvart" to svar.ubesvart,
            "string" to
                svar.verdi.takeIf {
                    it != null && svarType is Svar.StringSvar
                },
            "dato" to
                svar.verdi.takeIf {
                    it != null && svarType is Svar.LocalDateSvar
                },
            "heltall" to
                svar.verdi.takeIf {
                    it != null && svarType is Svar.IntegerSvar
                },
            "boolsk" to
                svar.verdi.takeIf {
                    it != null && svarType is Svar.BooleanSvar
                },
            "desimal" to
                svar.verdi.takeIf {
                    it != null && svarType is Svar.DoubleSvar
                },
            "rolle" to rolle?.name,
            "krever_totrinnskontroll" to kreverTotrinnskontroll,
        )
    }
}

internal class RelasjonStatementBuilder(oppgave: Oppgave) : OppgaveVisitor, StatementBuilder {
    private lateinit var _updateQueryActions: List<UpdateQueryAction>

    init {
        oppgave.accept(this)
    }

    override fun visit(behandling: Behandling) {
        _updateQueryActions =
            behandling.alleSteg().flatMap { parent ->
                parent.avhengigeSteg().map { children -> Pair(parent, children) }
            }.toSet().map {
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO steg_relasjon(behandling_id, parent_id, child_id)
                        VALUES (:behandling_id, :parent_id, :child_id)
                        ON CONFLICT (behandling_id,parent_id,child_id) DO NOTHING 
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "behandling_id" to behandling.uuid,
                            "parent_id" to it.first.uuid,
                            "child_id" to it.second.uuid,
                        ),
                ).asUpdate
            }
    }

    override val updateQueryActions: List<UpdateQueryAction> = _updateQueryActions
}

internal class SporingStatementBuilder(oppgave: Oppgave) : OppgaveVisitor, StatementBuilder {
    private lateinit var _updateQueryActions: List<UpdateQueryAction>

    init {
        oppgave.accept(this)
    }

    override fun visit(behandling: Behandling) {
        _updateQueryActions =
            behandling.alleSteg().filter { it.svar.sporing !is NullSporing }.map { steg ->
                val sporing = steg.svar.sporing
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO sporing(steg_uuid, utført, begrunnelse, utført_av, json, type)
                        VALUES (:steg_uuid, :utfort, :begrunnelse, :utfort_av, :json, :type)
                        ON CONFLICT(steg_uuid ) DO UPDATE SET utført      = :utfort,
                                                              begrunnelse = :begrunnelse,
                                                              utført_av   = :utfort_av,
                                                              json        = :json,
                                                              type        = :type
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "steg_uuid" to steg.uuid,
                        ) + sporing.toParamMap(),
                ).asUpdate
            }
    }

    override val updateQueryActions: List<UpdateQueryAction> = _updateQueryActions

    private fun Sporing.toParamMap(): Map<String, Any?> {
        val (utførtAv, begrunnelse) =
            when (this is ManuellSporing) {
                true -> Pair(this.utførtAv.ident, this.begrunnelse)
                else -> Pair(null, null)
            }

        val json =
            when (this is QuizSporing) {
                true ->
                    PGobject().also {
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
}
