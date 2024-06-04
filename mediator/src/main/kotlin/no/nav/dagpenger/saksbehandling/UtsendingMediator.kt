package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
import java.util.UUID

class UtsendingMediator(private val repository: UtsendingRepository) {
    fun startUtsending(oppgaveId: UUID) {
        val utsending = Utsending(oppgaveId)
        repository.lagre(utsending)
    }

    fun mottaBrev(vedtaksbrevHendelse: VedtaksbrevHendelse) {
        TODO("Not yet implemented")
    }
}
