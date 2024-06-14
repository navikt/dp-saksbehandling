package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.utsending.IngenBehov
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

private val logger = KotlinLogging.logger {}

class UtsendingMediator(private val repository: UtsendingRepository, private val rapidsConnection: RapidsConnection) {
    fun mottaBrev(vedtaksbrevHendelse: VedtaksbrevHendelse) {
        val utsending = repository.hentEllerOpprettUtsending(vedtaksbrevHendelse.oppgaveId)
        utsending.mottaBrev(vedtaksbrevHendelse)
        lagreOgPubliserBehov(utsending)
    }

    fun mottaStartUtsending(startUtsendingHendelse: StartUtsendingHendelse) {
        val utsending = repository.finnUtsendingFor(startUtsendingHendelse.oppgaveId)
        utsending?.let { utsending ->
            utsending.startUtsending(startUtsendingHendelse)
            lagreOgPubliserBehov(utsending)
        }

        if (utsending == null) {
            logger.info {
                "Fant ingen utsending for behandlingId:${startUtsendingHendelse.behandlingId}," +
                    " oppgaveId=${startUtsendingHendelse.oppgaveId}."
            }
        }
    }

    fun mottaUrnTilArkiverbartFormatAvBrev(arkiverbartBrevHendelse: ArkiverbartBrevHendelse) {
        val utsending = repository.hent(arkiverbartBrevHendelse.oppgaveId)
        utsending.mottaUrnTilArkiverbartFormatAvBrev(arkiverbartBrevHendelse)
        lagreOgPubliserBehov(utsending)
    }

    fun mottaJournalførtKvittering(journalførtHendelse: JournalførtHendelse) {
        val utsending = repository.hent(journalførtHendelse.oppgaveId)
        utsending.mottaJournalførtKvittering(journalførtHendelse)
        lagreOgPubliserBehov(utsending)
    }

    fun mottaDistribuertKvittering(distribuertHendelse: DistribuertHendelse) {
        val utsending = repository.hent(distribuertHendelse.oppgaveId)
        utsending.mottaDistribuertKvittering(distribuertHendelse)
        lagreOgPubliserBehov(utsending)
    }

    private fun lagreOgPubliserBehov(utsending: Utsending) {
        repository.lagre(utsending)
        publiserBehov(utsending)
    }

    private fun publiserBehov(utsending: Utsending) {
        val behov = utsending.tilstand().behov(utsending)
        if (behov is IngenBehov) return

        rapidsConnection.publish(JsonMessage.newNeed(setOf(behov.navn), behov.data()).toJson())
    }
}
