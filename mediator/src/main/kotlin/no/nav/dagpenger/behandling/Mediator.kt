package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.BehandlingSvar
import no.nav.dagpenger.behandling.persistence.BehandlingRepository
import no.nav.dagpenger.behandling.persistence.Inmemory
import java.lang.RuntimeException
import java.time.LocalDate

class Mediator(
    private val behandlingRepository: BehandlingRepository = Inmemory,

) : BehandlingRepository by behandlingRepository {
    fun behandle(hendelse: BehandlingSvar) {
        val behandling = hentBehandling(hendelse.behandlinUUID)
        when (hendelse.type) {
            "String" -> {
                behandling.besvar(hendelse.stegUUID, hendelse.verdi)
            }

            "LocalDate" -> {
                behandling.besvar(hendelse.stegUUID, LocalDate.parse(hendelse.verdi))
            }

            "Int" -> {
                behandling.besvar(hendelse.stegUUID, hendelse.verdi.toInt())
            }

            "Boolean" -> {
                behandling.besvar(hendelse.stegUUID, hendelse.verdi.toBoolean())
            }

            else -> throw RuntimeException("Todo")
        }
    }
}
