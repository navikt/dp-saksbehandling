package no.nav.dagpenger.saksbehandling.utsending.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.mottak.AbstractBehandlingsresultatMottak
import no.nav.dagpenger.saksbehandling.mottak.Behandlingsresultat
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator

private val logger = KotlinLogging.logger {}

internal class BehandlingsresultatMottakForUtsending(
    rapidsConnection: RapidsConnection,
    private val utsendingMediator: UtsendingMediator,
    private val sakRepository: SakRepository,
) : AbstractBehandlingsresultatMottak(rapidsConnection) {
    override fun requiredBehandletHendelseType(): List<String> = listOf("Søknad", "Manuell", "Meldekort")

    override val mottakNavn: String = "BehandlingsresultatMottakForUtsending"

    override fun requiredEventNames(): List<String> = listOf("behandlingsresultat", "dp_saksbehandling_behandlingsresultat_retry")

    override fun håndter(
        behandlingsresultat: Behandlingsresultat,
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val dagpengerSakId by lazy {
            try {
                sakRepository.hentDagpengerSakIdForBehandlingId(behandlingsresultat.behandlingId)
            } catch (e: Exception) {
                null
            }
        }

        val skalStarteUtsending =
            (behandlingsresultat.nyDagpengerettInnvilget() || dagpengerSakId != null).also {
                logger.info {
                    "BehandlingsresultatMottakForUtsending med utfall: $it. Basert på $behandlingsresultat".also { msg ->
                        if (!behandlingsresultat.nyDagpengerettInnvilget()) {
                            msg.plus("DagpengerSakId: $dagpengerSakId")
                        }
                    }
                }
            }

        if (skalStarteUtsending) {
            val sakId = sakRepository.hentSakIdForBehandlingId(behandlingsresultat.behandlingId).toString()
            val vedtakFattetHendelse =
                packet.vedtakFattetHendelse(
                    sak =
                        UtsendingSak(
                            id = sakId,
                            kontekst = "Dagpenger",
                        ),
                    behandlingsresultat = behandlingsresultat,
                )

            utsendingMediator.startUtsendingForVedtakFattet(
                vedtakFattetHendelse = vedtakFattetHendelse,
            )
        }
    }
}
