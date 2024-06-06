package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

class UtsendingMediator(private val repository: UtsendingRepository, private val rapidsConnection: RapidsConnection) {
    fun mottaBrev(vedtaksbrevHendelse: VedtaksbrevHendelse) {
        val utsending =
            repository.finnUtsendingFor(vedtaksbrevHendelse.oppgaveId) ?: Utsending(oppgaveId = vedtaksbrevHendelse.oppgaveId)
        utsending.mottaBrev(vedtaksbrevHendelse)
        lagreOgPubliserBehov(utsending)
    }

    fun mottaStartUtsending(startUtsendingHendelse: StartUtsendingHendelse) {
        val utsending = repository.hentUtsendingFor(startUtsendingHendelse.behandlingId)
        utsending.startUtsending(startUtsendingHendelse)
        lagreOgPubliserBehov(utsending)
    }

    private fun lagreOgPubliserBehov(utsending: Utsending) {
        repository.lagre(utsending)
        publiserBehov(utsending)
    }

    private fun publiserBehov(utsending: Utsending) {
        val behov = utsending.tilstand().behov2(utsending)
        if (behov.isEmpty()) return

        rapidsConnection.publish(JsonMessage.newNeed(behov.keys, behov.entries.first().value).toJson())
    }
}
