package no.nav.dagpenger.behandling.oppgave

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.IBehandling
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
) : IArbeidsprosess by prosess, IBehandling by behandling {
    constructor(behandling: Behandling, prosess: Arbeidsprosess) : this(
        UUID.randomUUID(),
        behandling,
        prosess,
        null,
        LocalDateTime.now(),
    )

    val person get() = behandling.person

    fun alleSteg() = behandling.alleSteg()
    fun muligeTilstander() = prosess.muligeTilstander()
}
/*
POST /oppgave/123/behandling/svar/UUID
GET /oppgave/123/ -> { gyldigeTilstander: ["VentPåMangelbrev", "Innstilt"]  }
POST /oppgave/123/ { gåTilTilstand: "VentPåMangelbrev" }
 */
