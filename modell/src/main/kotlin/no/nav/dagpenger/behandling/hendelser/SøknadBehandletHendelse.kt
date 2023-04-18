package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.Behandling

class SøknadBehandletHendelse(
    val behandling: Behandling,
    val innvilget: Boolean,
) : Hendelse(behandling.person.ident) {
    fun toJsonMessageMap(): Map<String, Any> {
        return mapOf(
            "behandlingId" to behandling.uuid,
            "innvilget" to innvilget,
            "ident" to behandling.person.ident,
        ) + behandling.fastsettelser()
    }
}
