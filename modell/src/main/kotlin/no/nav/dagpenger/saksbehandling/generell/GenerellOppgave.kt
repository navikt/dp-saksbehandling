package no.nav.dagpenger.saksbehandling.generell

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import java.time.LocalDateTime
import java.util.UUID

class GenerellOppgave private constructor(
    val id: UUID = UUIDv7.ny(),
    val person: Person,
    val tittel: String,
    val beskrivelse: String = "",
    val strukturertData: JsonNode = NullNode.instance,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    private var vurdering: String? = null,
    private var tilstand: Tilstand = Tilstand.BEHANDLES,
    private var resultat: Resultat = Resultat.Ingen,
    private var valgtSakId: UUID? = null,
) {
    companion object {
        fun opprett(
            person: Person,
            tittel: String,
            beskrivelse: String = "",
            strukturertData: JsonNode = NullNode.instance,
        ): GenerellOppgave =
            GenerellOppgave(
                person = person,
                tittel = tittel,
                beskrivelse = beskrivelse,
                strukturertData = strukturertData,
            )

        fun rehydrer(
            id: UUID,
            person: Person,
            tittel: String,
            beskrivelse: String,
            strukturertData: JsonNode,
            opprettet: LocalDateTime,
            tilstand: String,
            vurdering: String?,
            resultat: Resultat,
            valgtSakId: UUID?,
        ): GenerellOppgave =
            GenerellOppgave(
                id = id,
                person = person,
                tittel = tittel,
                beskrivelse = beskrivelse,
                strukturertData = strukturertData,
                opprettet = opprettet,
                tilstand = Tilstand.valueOf(tilstand),
                vurdering = vurdering,
                resultat = resultat,
                valgtSakId = valgtSakId,
            )
    }

    fun vurdering(): String? = vurdering

    fun tilstand() = tilstand.name

    fun resultat() = resultat

    fun valgtSakId() = valgtSakId

    private enum class Tilstand {
        BEHANDLES,
        FERDIGSTILL_STARTET,
        FERDIGSTILT,
    }

    sealed class Resultat {
        data object Ingen : Resultat()

        data class Klage(
            val behandlingId: UUID,
        ) : Resultat()

        data class RettTilDagpenger(
            val behandlingId: UUID,
        ) : Resultat()
    }

    fun startFerdigstilling(
        vurdering: String?,
        valgtSakId: UUID?,
    ) {
        this.vurdering = vurdering
        this.valgtSakId = valgtSakId
        this.tilstand = Tilstand.FERDIGSTILL_STARTET
    }

    fun ferdigstill(
        aksjonType: GenerellOppgaveAksjon.Type,
        opprettetBehandlingId: UUID? = null,
    ) {
        when (aksjonType) {
            GenerellOppgaveAksjon.Type.AVSLUTT -> {
                this.resultat = Resultat.Ingen
            }

            GenerellOppgaveAksjon.Type.OPPRETT_KLAGE -> {
                requireNotNull(opprettetBehandlingId) {
                    "behandlingId kan ikke være null etter opprettelse av klage"
                }
                this.resultat = Resultat.Klage(opprettetBehandlingId)
            }

            GenerellOppgaveAksjon.Type.OPPRETT_MANUELL_BEHANDLING -> {
                requireNotNull(opprettetBehandlingId) {
                    "behandlingId kan ikke være null etter opprettelse av manuell behandling"
                }
                this.resultat = Resultat.RettTilDagpenger(opprettetBehandlingId)
            }

            GenerellOppgaveAksjon.Type.OPPRETT_REVURDERING_BEHANDLING -> {
                requireNotNull(opprettetBehandlingId) {
                    "behandlingId kan ikke være null etter opprettelse av revurdering"
                }
                this.resultat = Resultat.RettTilDagpenger(opprettetBehandlingId)
            }
        }
        this.tilstand = Tilstand.FERDIGSTILT
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GenerellOppgave

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "GenerellOppgave(id=$id, tittel='$tittel', tilstand=$tilstand)"
}
