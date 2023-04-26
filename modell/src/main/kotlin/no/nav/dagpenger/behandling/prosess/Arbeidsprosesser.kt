package no.nav.dagpenger.behandling.prosess

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.prosess.Arbeidsprosess.Overgang

object Arbeidsprosesser {
    fun totrinnsprosess(behandling: Behandling) = Arbeidsprosess().apply {
        leggTilTilstand(
            "TilBehandling",
            Overgang("Innstilt") { behandling.erFerdig() },
            Overgang("VentPåMangelbrev"),
        )
        leggTilTilstand(
            "Innstilt",
            Overgang("TilBehandling"),
            Overgang(
                "Vedtak",
                vedOvergang = {
                    behandling.håndter(InnstillingGodkjentHendelse(behandling.person.ident))
                },
            ),
        )
        leggTilTilstand("VentPåMangelbrev", Overgang("TilBehandling"))
        leggTilTilstand("Vedtak")
    }
}

class InnstillingGodkjentHendelse(ident: String) : Hendelse(ident)
