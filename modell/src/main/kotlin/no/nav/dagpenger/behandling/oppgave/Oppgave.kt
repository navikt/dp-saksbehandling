package no.nav.dagpenger.behandling.oppgave

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.BehandlingObserver
import no.nav.dagpenger.behandling.Behandlingsstatus
import no.nav.dagpenger.behandling.Svarbart
import no.nav.dagpenger.behandling.prosess.Arbeidsprosess
import no.nav.dagpenger.behandling.prosess.IArbeidsprosess
import java.time.LocalDateTime
import java.util.UUID

// Ansvar for hvem som skal utføre behandling
data class Oppgave private constructor(
    val uuid: UUID,
    private val behandling: Behandling,
    private val prosess: Arbeidsprosess,
    val utføresAv: Saksbehandler?,
    val opprettet: LocalDateTime,
) : IArbeidsprosess by prosess, Svarbart by behandling, Behandlingsstatus by behandling {
    constructor(behandling: Behandling, prosess: Arbeidsprosess) : this(
        UUID.randomUUID(),
        behandling,
        prosess,
        null,
        LocalDateTime.now(),
    )

    val person get() = behandling.person

    val behandler = behandling.behandler
    fun alleSteg() = behandling.alleSteg()
    fun nesteSteg() = behandling.nesteSteg()
    fun muligeTilstander() = prosess.muligeTilstander()
    fun steg(uuid: UUID) = behandling.steg.single { it.uuid == uuid }
    fun addObserver(observer: BehandlingObserver) = behandling.addObserver(observer)
}
