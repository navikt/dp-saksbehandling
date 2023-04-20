package no.nav.dagpenger.behandling.oppgave

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.prosess.Arbeidsprosess
import no.nav.dagpenger.behandling.prosess.IArbeidsprosess
import java.time.LocalDate
import java.util.UUID

// Ansvar for hvem som skal utføre behandling
data class Oppgave private constructor(
    val uuid: UUID,
    private val behandling: Behandling,
    private val prosess: Arbeidsprosess,
    val utføresAv: Saksbehandler?,
) : IArbeidsprosess by prosess {
    constructor(behandling: Behandling, prosess: Arbeidsprosess) : this(UUID.randomUUID(), behandling, prosess, null)

    fun besvar(uuid: UUID, verdi: String) = behandling.besvar(uuid, verdi)
    fun besvar(uuid: UUID, verdi: Int) = behandling.besvar(uuid, verdi)
    fun besvar(uuid: UUID, verdi: LocalDate) = behandling.besvar(uuid, verdi)
    fun besvar(uuid: UUID, verdi: Boolean) = behandling.besvar(uuid, verdi)
}

class Saksbehandler
/*
POST /oppgave/123/behandling/svar/UUID
GET /oppgave/123/ -> { gyldigeTilstander: ["VentPåMangelbrev", "Innstilt"]  }
POST /oppgave/123/ { gåTilTilstand: "VentPåMangelbrev" }
 */
