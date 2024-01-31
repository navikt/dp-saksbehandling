package no.nav.dagpenger.behandling.oppgave

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.BehandlingObserver
import no.nav.dagpenger.behandling.Behandlingsstatus
import no.nav.dagpenger.behandling.OppgaveVisitor
import no.nav.dagpenger.behandling.Saksbehandler
import no.nav.dagpenger.behandling.UtførStegKommando
import no.nav.dagpenger.behandling.hendelser.VurderAvslagPåMinsteinntektHendelse
import java.time.LocalDateTime
import java.util.UUID

// Ansvar for hvem som skal utføre behandling
data class Oppgave private constructor(
    val uuid: UUID,
    private val behandling: Behandling,
    val utføresAv: Saksbehandler?,
    val opprettet: LocalDateTime,
    private val _emneknagger: MutableSet<String>,
) : Behandlingsstatus by behandling {
    constructor(uuid: UUID, behandling: Behandling, emneknagger: Set<String> = emptySet()) : this(
        uuid = uuid,
        behandling = behandling,
        utføresAv = null,
        opprettet = LocalDateTime.now(),
        _emneknagger = emneknagger.toMutableSet(),
    )

    val emneknagger: Set<String>
        get() = _emneknagger.toSet()

    companion object {
        fun rehydrer(
            uuid: UUID,
            behandling: Behandling,
            utføresAv: String?,
            opprettet: LocalDateTime,
            emneknagger: Set<String>,
        ) = Oppgave(
            uuid = uuid,
            behandling = behandling,
            utføresAv =
                utføresAv?.let {
                    Saksbehandler(it)
                },
            opprettet = opprettet,
            _emneknagger = emneknagger.toMutableSet(),
        )
    }

    fun accept(visitor: OppgaveVisitor) {
        visitor.visit(uuid, opprettet, utføresAv, emneknagger.toSet())
        visitor.visit(behandling)
        behandling.accept(visitor)
    }

    val person get() = behandling.person

    val behandler = behandling.behandler

    fun alleSteg() = behandling.alleSteg()

    fun nesteSteg() = behandling.nesteSteg()

    fun steg(uuid: UUID) = behandling.steg.single { it.uuid == uuid }

    fun steg(id: String) = behandling.steg.single { it.id == id }

    fun addObserver(observer: BehandlingObserver) = behandling.addObserver(observer)

    fun utfør(kommando: UtførStegKommando) {
        behandling.utfør(kommando)
    }

    fun behandle(vurderAvslagPåMinsteinntektHendelse: VurderAvslagPåMinsteinntektHendelse) {
        this._emneknagger.add("VurderAvslagPåMinsteinntekt")
    }

    val tilstand: OppgaveTilstand
        get() =
            when (behandling.erBehandlet()) {
                true -> OppgaveTilstand.FerdigBehandlet
                false -> OppgaveTilstand.TilBehandling
            }
}

enum class OppgaveTilstand {
    TilBehandling,
    FerdigBehandlet,
}
