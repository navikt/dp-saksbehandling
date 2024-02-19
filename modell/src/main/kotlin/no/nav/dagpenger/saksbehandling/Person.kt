package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VerifiserOpplysningHendelse
import java.util.UUID

data class Person(val ident: String) {
    val behandlinger = mutableMapOf<UUID, Behandling>()

    init {
        require(ident.matches(Regex("\\d{11}"))) { "Person-ident må ha 11 siffer, fikk ${ident.length}" }
    }

    fun håndter(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        val behandling = Behandling(behandlingOpprettetHendelse.behandlingId)
        behandlinger.put(behandling.behandlingId, behandling)
    }

    fun håndter(verifiserOpplysningHendelse: VerifiserOpplysningHendelse) {
        val behandling = behandlinger.get(verifiserOpplysningHendelse.behandlingId)
        if (behandling != null) {
            behandling.håndter(verifiserOpplysningHendelse)
        } else {
            throw IllegalStateException(
                "Fant ikke behandling med id: ${verifiserOpplysningHendelse.behandlingId} ved håndtering av " +
                    "verifiser_opplysning hendelse",
            )
        }
    }
}
