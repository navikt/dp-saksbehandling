package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import java.util.UUID

data class Person(val ident: String) {
    val behandlinger = mutableMapOf<UUID, Behandling>()

    init {
        require(ident.matches(Regex("\\d{11}"))) { "Person-ident må ha 11 siffer, fikk ${ident.length}" }
    }

    fun håndter(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        val behandling =
            Behandling(
                behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
                oppgave =
                    Oppgave(
                        oppgaveId = UUIDv7.ny(),
                        ident = søknadsbehandlingOpprettetHendelse.ident,
                        emneknagger = setOf("Søknadsbehandling"),
                        opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
                    ),
            )
        behandlinger.put(behandling.behandlingId, behandling)
    }
}
