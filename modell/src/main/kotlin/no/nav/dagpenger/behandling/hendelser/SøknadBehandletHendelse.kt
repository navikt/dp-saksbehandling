package no.nav.dagpenger.behandling.hendelser

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.behandling.Behandling

class SøknadBehandletHendelse(
    val behandling: Behandling,
    val innvilget: Boolean,
) : Hendelse(behandling.person.ident) {

    companion object {
        private val objectmapper = jacksonObjectMapper()
    }

    fun toJson(): String {
        val event = mapOf(
            "@event_name" to "søknad_behandlet_hendelse",
            "behandlingId" to behandling.uuid,
            "innvilget" to innvilget,
            "ident" to behandling.person.ident,
        ) + behandling.fastsettelser()
        return objectmapper.writeValueAsString(event)
    }
}
