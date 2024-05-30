package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.utsending.Utsending
import java.util.UUID

class UtsendingMediator {
    fun startUtsending(oppgaveId: UUID) {
        val utsending = Utsending(oppgaveId)
    }
}
