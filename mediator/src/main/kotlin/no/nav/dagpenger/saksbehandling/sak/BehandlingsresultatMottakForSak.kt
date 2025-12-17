package no.nav.dagpenger.saksbehandling.sak

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
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class BehandlingsresultatMottakForSak(
    rapidsConnection: RapidsConnection,
    private val sakRepository: SakRepository,
    private val sakMediator: SakMediator,
) : AbstractBehandlingsresultatMottak(rapidsConnection) {
    override fun requiredBehandletHendelseType(): List<String> = listOf("Søknad")

    override val mottakNavn: String = "BehandlingsresultatMottakForSak"

    override fun håndter(
        behandlingsresultat: Behandlingsresultat,
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        if (behandlingsresultat.nyDagpengerettInnvilget().also {
                logger.info { "BehandlingsresultatMottakForSak med utfall: $it. Basert på $behandlingsresultat" }
            }
        ) {
            val sakId = sakRepository.hentSakIdForBehandlingId(behandlingsresultat.behandlingId).toString()
            logger.info { "Vedtak skal tilhøre dp-sak " }
            val vedtakFattetHendelse =
                packet.vedtakFattetHendelse(
                    sak =
                        UtsendingSak(
                            id = sakId,
                            kontekst = "Dagpenger",
                        ),
                    behandlingsresultat = behandlingsresultat,
                )

            sakMediator.merkSakenSomDpSak(vedtakFattetHendelse = vedtakFattetHendelse)
            context.publish(
                key = packet["ident"].asText(),
                message =
                    JsonMessage
                        .newMessage(
                            map =
                                VedtakUtenforArena(
                                    behandlingId = behandlingsresultat.behandlingId,
                                    søknadId = behandlingsresultat.behandletHendelseId,
                                    ident = packet["ident"].asText(),
                                    sakId = sakId,
                                ).toMap(),
                        ).toJson(),
            )
        }
    }

    private data class VedtakUtenforArena(
        val behandlingId: UUID,
        val søknadId: String,
        val ident: String,
        val sakId: String,
    ) {
        fun toMap(): Map<String, String> =
            mapOf(
                "@event_name" to "vedtak_fattet_utenfor_arena",
                "behandlingId" to behandlingId.toString(),
                "søknadId" to søknadId,
                "ident" to ident,
                "sakId" to sakId,
            )
    }
}
