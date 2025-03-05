package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class UtsendingMediator(
    private val repository: UtsendingRepository,
) : UtsendingRepository by repository {
    private lateinit var rapidsConnection: RapidsConnection

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

    fun opprettUtsending(
        oppgaveId: UUID,
        brev: String,
        ident: String,
    ): UUID {
        val utsending =
            Utsending(
                oppgaveId = oppgaveId,
                ident = ident,
                brev = brev,
            )
        repository.lagre(utsending)
        return utsending.id
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

        val message =
            JsonMessage.newNeed(setOf(behov.navn), behov.data()).toJson().also {
                sikkerlogg.info { "Publiserer behov: $it for $utsending" }
            }
        logger.info { "Publiserer behov: ${behov.navn} for $utsending" }
        rapidsConnection.publish(key = utsending.ident, message = message)
    }
}
