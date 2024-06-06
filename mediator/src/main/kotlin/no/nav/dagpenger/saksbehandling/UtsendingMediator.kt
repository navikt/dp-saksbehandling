package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse

class UtsendingMediator(private val repository: UtsendingRepository) {
    fun mottaBrev(vedtaksbrevHendelse: VedtaksbrevHendelse) {
        val utsending =
            repository.finnUtsendingFor(vedtaksbrevHendelse.oppgaveId) ?: Utsending(oppgaveId = vedtaksbrevHendelse.oppgaveId)
        utsending.mottaBrev(vedtaksbrevHendelse)
        repository.lagre(utsending)
    }

    fun mottaStartUtsending(startUtsendingHendelse: StartUtsendingHendelse) {
        val utsending = repository.hentUtsendingFor(startUtsendingHendelse.behandlingId)
        utsending.startUtsending(startUtsendingHendelse)
        repository.lagre(utsending)
    }
}
