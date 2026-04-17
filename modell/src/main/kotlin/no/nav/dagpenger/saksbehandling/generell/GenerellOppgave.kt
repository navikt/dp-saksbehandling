package no.nav.dagpenger.saksbehandling.generell

import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GenerellOppgave private constructor(
    val id: UUID = UUIDv7.ny(),
    val person: Person,
    val tittel: String,
    val beskrivelse: String = "",
    val strukturertData: Map<String, Any> = emptyMap(),
    val frist: LocalDate? = null,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    private var vurdering: String? = null,
    private var tilstand: Tilstand = Tilstand.BEHANDLES,
    private var resultat: Resultat = Resultat.Ingen,
    private var valgtSakId: UUID? = null,
) {
    companion object {
        fun opprett(
            id: UUID = UUIDv7.ny(),
            person: Person,
            tittel: String,
            beskrivelse: String = "",
            strukturertData: Map<String, Any> = emptyMap(),
            frist: LocalDate? = null,
            opprettet: LocalDateTime = LocalDateTime.now(),
        ): GenerellOppgave =
            GenerellOppgave(
                id = id,
                person = person,
                tittel = tittel,
                beskrivelse = beskrivelse,
                strukturertData = strukturertData,
                frist = frist,
                opprettet = opprettet,
            )

        fun rehydrer(
            id: UUID,
            person: Person,
            tittel: String,
            beskrivelse: String,
            strukturertData: Map<String, Any>,
            frist: LocalDate?,
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
                frist = frist,
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

        data class GenerellOppgave(
            val behandlingId: UUID,
        ) : Resultat()
    }

    fun startFerdigstilling(
        vurdering: String?,
        valgtSakId: UUID?,
    ) {
        if (tilstand != Tilstand.BEHANDLES) {
            throw UlovligTilstandsendringException("Kan ikke starte ferdigstilling fra tilstand $tilstand")
        }
        this.vurdering = vurdering
        this.valgtSakId = valgtSakId
        this.tilstand = Tilstand.FERDIGSTILL_STARTET
    }

    fun ferdigstill(
        aksjonType: GenerellOppgaveAksjon.Type,
        opprettetBehandlingId: UUID? = null,
    ) {
        if (tilstand != Tilstand.FERDIGSTILL_STARTET) {
            throw UlovligTilstandsendringException("Kan ikke ferdigstille fra tilstand $tilstand")
        }
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

            GenerellOppgaveAksjon.Type.OPPRETT_GENERELL_OPPGAVE -> {
                requireNotNull(opprettetBehandlingId) {
                    "behandlingId kan ikke være null etter opprettelse av generell oppgave"
                }
                this.resultat = Resultat.GenerellOppgave(opprettetBehandlingId)
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

    class UlovligTilstandsendringException(
        message: String,
    ) : RuntimeException(message)
}
