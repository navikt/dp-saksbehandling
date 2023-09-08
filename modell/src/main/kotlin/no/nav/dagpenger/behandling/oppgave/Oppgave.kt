package no.nav.dagpenger.behandling.oppgave

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.BehandlingObserver
import no.nav.dagpenger.behandling.Behandlingsstatus
import no.nav.dagpenger.behandling.OppgaveVisitor
import no.nav.dagpenger.behandling.UtførStegKommando
import java.time.LocalDateTime
import java.util.UUID

// Ansvar for hvem som skal utføre behandling
data class Oppgave private constructor(
    val uuid: UUID,
    private val behandling: Behandling,
    val utføresAv: Saksbehandler?,
    val opprettet: LocalDateTime,
) : Behandlingsstatus by behandling {
    constructor(uuid: UUID, behandling: Behandling) : this(
        uuid,
        behandling,
        null,
        LocalDateTime.now(),
    )

    fun accept(visitor: OppgaveVisitor) {
        visitor.visit(uuid)
        visitor.visit(behandling)
        behandling.accept(visitor)
    }

    val person get() = behandling.person

    val behandler = behandling.behandler
    fun alleSteg() = behandling.alleSteg()
    fun nesteSteg() = behandling.nesteSteg()
    fun steg(uuid: UUID) = behandling.steg.single { it.uuid == uuid }
    fun addObserver(observer: BehandlingObserver) = behandling.addObserver(observer)

    fun utfør(kommando: UtførStegKommando) {
        behandling.utfør(kommando)
    }

    val tilstand: OppgaveTilstand
        get() = when (behandling.erBehandlet()) {
            true -> OppgaveTilstand.FerdigBehandlet
            false -> OppgaveTilstand.TilBehandling
        }
}

enum class OppgaveTilstand {
    TilBehandling,
    FerdigBehandlet,
}
