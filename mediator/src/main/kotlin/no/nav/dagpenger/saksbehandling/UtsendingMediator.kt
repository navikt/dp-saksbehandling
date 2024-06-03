package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import java.util.UUID

class UtsendingMediator(private val repository: UtsendingRepository) {
    fun startUtsending(oppgaveId: UUID) {
        val utsending = Utsending(oppgaveId)
    }
}
