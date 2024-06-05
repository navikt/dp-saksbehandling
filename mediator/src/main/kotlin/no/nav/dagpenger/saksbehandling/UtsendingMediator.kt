package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse

class UtsendingMediator(private val repository: UtsendingRepository) {
    fun mottaBrev(vedtaksbrevHendelse: VedtaksbrevHendelse) {
        val utsending =
            repository.finnUtsendingFor(vedtaksbrevHendelse.oppgaveId) ?: Utsending(vedtaksbrevHendelse.oppgaveId)
        utsending.mottaBrev(vedtaksbrevHendelse)
        repository.lagre(utsending)
    }

    fun mottaVedtakFattet(vedtakFattetHendelse: VedtakFattetHendelse) {
        val utsending = repository.hentUtsendingFor(vedtakFattetHendelse.behandlingId)
        utsending.mottaVedtak(vedtakFattetHendelse)
        repository.lagre(utsending)
    }
}
