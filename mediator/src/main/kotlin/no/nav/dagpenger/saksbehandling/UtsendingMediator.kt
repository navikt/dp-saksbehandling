package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.utsending.IngenBehov
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalpostHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.MidlertidigJournalpostHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

class UtsendingMediator(private val repository: UtsendingRepository, private val rapidsConnection: RapidsConnection) {
    fun mottaBrev(vedtaksbrevHendelse: VedtaksbrevHendelse) {
        val utsending =
            repository.finnUtsendingFor(vedtaksbrevHendelse.oppgaveId)
                ?: Utsending(oppgaveId = vedtaksbrevHendelse.oppgaveId)
        utsending.mottaBrev(vedtaksbrevHendelse)
        lagreOgPubliserBehov(utsending)
    }

    fun mottaStartUtsending(startUtsendingHendelse: StartUtsendingHendelse) {
        val utsending = repository.hent(startUtsendingHendelse.oppgaveId)
        utsending.startUtsending(startUtsendingHendelse)
        lagreOgPubliserBehov(utsending)
    }

    fun mottaUrnTilArkiverbartFormatAvBrev(arkiverbartBrevHendelse: ArkiverbartBrevHendelse) {
        val utsending = repository.hent(arkiverbartBrevHendelse.oppgaveId)
        utsending.mottaUrnTilArkiverbartFormatAvBrev(arkiverbartBrevHendelse)
        lagreOgPubliserBehov(utsending)
    }

    fun mottaMidleridigJournalpost(midlertidigJournalpostHendelse: MidlertidigJournalpostHendelse) {
        val utsending = repository.hent(midlertidigJournalpostHendelse.oppgaveId)
        utsending.mottaMidlertidigJournalpost(midlertidigJournalpostHendelse)
        lagreOgPubliserBehov(utsending)
    }

    fun mottaJournalpost(journalpostHendelse: JournalpostHendelse) {
        val utsending = repository.hent(journalpostHendelse.oppgaveId)
        utsending.mottaJournalpost(journalpostHendelse)
        lagreOgPubliserBehov(utsending)
    }

    private fun lagreOgPubliserBehov(utsending: Utsending) {
        repository.lagre(utsending)
        publiserBehov(utsending)
    }

    private fun publiserBehov(utsending: Utsending) {
        val behov = utsending.tilstand().behov(utsending)
        if (behov is IngenBehov) return

        rapidsConnection.publish(JsonMessage.newNeed(setOf(behov.navn), behov.data).toJson())
    }
}
