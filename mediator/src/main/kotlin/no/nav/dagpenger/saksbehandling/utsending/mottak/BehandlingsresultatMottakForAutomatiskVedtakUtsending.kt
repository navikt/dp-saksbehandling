package no.nav.dagpenger.saksbehandling.utsending.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import java.util.UUID
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.mottak.AbstractBehandlingsresultatMottak
import no.nav.dagpenger.saksbehandling.mottak.Behandlingsresultat
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator

private val logger = KotlinLogging.logger {}

internal class BehandlingsresultatMottakForAutomatiskVedtakUtsending(
    rapidsConnection: RapidsConnection,
    private val utsendingMediator: UtsendingMediator,
    private val sakRepository: SakRepository,
) : AbstractBehandlingsresultatMottak(rapidsConnection) {
    override fun requiredBehandletHendelseType(): List<String> = listOf("Søknad")

    override val mottakNavn: String = "BehandlingsresultatMottakForAutomatiskVedtakUtsending"

    override fun JsonMessage.valideringsregler() {
        this.requireValue("automatisk", true)
    }

    override fun håndter(
        behandlingsresultat: Behandlingsresultat,
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val skipSetBehandlingId = setOf<UUID>(UUID.fromString("01a05bcc-9ee5-7175-9d57-ef52e15ffa9b"))
        if (behandlingsresultat.behandlingId in skipSetBehandlingId) {
            logger.info { "Skipper behandlingId: ${behandlingsresultat.behandlingId} fra " +
                    "BehandlingsresultatMottakForAutomatiskVedtakUtsending" }
            return
        }
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
        utsendingMediator.startUtsendingForAutomatiskVedtakFattet(vedtakFattetHendelse = vedtakFattetHendelse)
    }
}
