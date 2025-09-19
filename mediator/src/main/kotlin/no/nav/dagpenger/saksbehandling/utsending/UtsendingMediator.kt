package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.VenterPåVedtak
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class UtsendingMediator(
    private val utsendingRepository: UtsendingRepository,
    private val brevProdusent: BrevProdusent,
) : UtsendingRepository by utsendingRepository {
    private lateinit var rapidsConnection: RapidsConnection

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

    fun opprettUtsending(
        behandlingId: UUID,
        brev: String?,
        ident: String,
        type: UtsendingType = UtsendingType.VEDTAK_DAGPENGER,
    ): UUID {
        val utsending =
            Utsending(
                behandlingId = behandlingId,
                ident = ident,
                brev = brev,
                type = type,
            )
        utsendingRepository.lagre(utsending)
        return utsending.id
    }

    fun mottaStartUtsending(startUtsendingHendelse: StartUtsendingHendelse) {
        val utsending = utsendingRepository.hentUtsendingForBehandlingId(startUtsendingHendelse.behandlingId)
        utsending.startUtsending(startUtsendingHendelse)
        lagreOgPubliserBehov(utsending)
    }

    fun mottaUrnTilArkiverbartFormatAvBrev(arkiverbartBrevHendelse: ArkiverbartBrevHendelse) {
        val utsending = utsendingRepository.hentUtsendingForBehandlingId(arkiverbartBrevHendelse.behandlingId)
        utsending.mottaUrnTilArkiverbartFormatAvBrev(arkiverbartBrevHendelse)
        lagreOgPubliserBehov(utsending)
    }

    fun mottaJournalførtKvittering(journalførtHendelse: JournalførtHendelse) {
        val utsending = utsendingRepository.hentUtsendingForBehandlingId(journalførtHendelse.behandlingId)
        utsending.mottaJournalførtKvittering(journalførtHendelse)
        lagreOgPubliserBehov(utsending)
    }

    fun mottaDistribuertKvittering(distribuertHendelse: DistribuertHendelse) {
        val utsending = utsendingRepository.hentUtsendingForBehandlingId(distribuertHendelse.behandlingId)
        utsending.mottaDistribuertKvittering(distribuertHendelse)
        lagreOgPubliserBehov(utsending)
    }

    private fun lagreOgPubliserBehov(utsending: Utsending) {
        utsendingRepository.lagre(utsending)
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

    fun startUtsendingForVedtakFattet(vedtakFattetHendelse: VedtakFattetHendelse) {
        val sak = vedtakFattetHendelse.sak
        require(sak != null) { "VedtakFattetHendelse må ha en sak" }
        utsendingRepository.finnUtsendingForBehandlingId(behandlingId = vedtakFattetHendelse.behandlingId)
            ?.let { utsending ->
                // TODO: Fjern sjekk på tilstand
                // Dette er en midlertidig løsning i dev, for å unngå at vi starter utsending på nytt fra
                // ArenaSinkVedtakOpprettetMottak når vedtak fattes i Arena
                if (utsending.tilstand().type == VenterPåVedtak) {
                    val brev =
                        runBlocking {
                            // todo håndter exception
                            brevProdusent.lagBrev(
                                ident = vedtakFattetHendelse.ident,
                                behandlingId = vedtakFattetHendelse.behandlingId,
                                sakId = sak.id,
                            )
                        }

                    utsending.startUtsending(
                        startUtsendingHendelse =
                            StartUtsendingHendelse(
                                utsendingSak = sak,
                                behandlingId = vedtakFattetHendelse.behandlingId,
                                ident = vedtakFattetHendelse.ident,
                                brev = brev,
                            ),
                    )
                    lagreOgPubliserBehov(utsending = utsending)

                    // TODO: Fjern logging og if/else
                } else {
                    logger.warn {
                        "Start utsending ble kalt for behandlingId=${vedtakFattetHendelse.behandlingId}, " +
                            "sakId=${sak.id}, sakKontekst=${sak.kontekst}.  " +
                            "Brev er allerede produsert, så gjør ingenting."
                    }
                }
            }
    }

    class BrevProdusent(
        private val oppslag: Oppslag,
        private val meldingOmVedtakKlient: MeldingOmVedtakKlient,
        private val oppgaveRepository: OppgaveRepository,
        private val tokenProvider: () -> String = Configuration.meldingOmVedtakMaskinTokenProvider,
    ) {
        suspend fun lagBrev(
            ident: String,
            behandlingId: UUID,
            sakId: String,
        ): String {
            return coroutineScope {
                val oppgave = oppgaveRepository.hentOppgaveFor(behandlingId)
                val person = async(Dispatchers.IO) { oppslag.hentPerson(ident) }

                // TODO: For automatiske vedtak må SB og beslutter håndteres annerledes, når vi kommer så langt
                val saksbehandler =
                    async(Dispatchers.IO) {
                        oppgave.sisteSaksbehandler()?.let { saksbehandlerIdent ->
                            oppslag.hentBehandler(saksbehandlerIdent)
                        } ?: throw RuntimeException("Fant ikke saksbehandler for oppgave ${oppgave.oppgaveId}")
                    }
                val beslutter =
                    async(Dispatchers.IO) {
                        oppgave.sisteBeslutter()?.let { beslutterIdent ->
                            oppslag.hentBehandler(beslutterIdent)
                        }
                    }

                meldingOmVedtakKlient.lagOgHentMeldingOmVedtakM2M(
                    person = person.await(),
                    saksbehandler = saksbehandler.await(),
                    beslutter = beslutter.await(),
                    behandlingId = behandlingId,
                    maskinToken = tokenProvider.invoke(),
                    utløstAv = oppgave.utløstAv,
                    sakId = sakId,
                ).getOrThrow()
            }
        }
    }
}
