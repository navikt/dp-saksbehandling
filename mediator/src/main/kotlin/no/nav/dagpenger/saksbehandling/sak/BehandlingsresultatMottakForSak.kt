package no.nav.dagpenger.saksbehandling.sak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.mottak.AbstractBehandlingResultatMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingResultat
import java.util.UUID
import kotlin.collections.toMap

private val logger = KotlinLogging.logger {}

internal class BehandlingsresultatMottakForSak(
    rapidsConnection: RapidsConnection,
    private val sakRepository: SakRepository,
    private val sakMediator: SakMediator,
) : AbstractBehandlingResultatMottak(rapidsConnection) {
    override fun requiredBehandletHendelseType(): List<String> = listOf("Søknad")

    override val mottakNavn: String = "BehandlingsresultatMottakForSak"

    override fun håndter(
        behandlingResultat: BehandlingResultat,
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        if (behandlingResultat.nyDagpengerettInnvilget().also {
                logger.info { "BehandlingsresultatMottakForSak med utfall: $it. Basert på $behandlingResultat" }
            }
        ) {
            val sakId = sakRepository.hentSakIdForBehandlingId(behandlingResultat.behandlingId).toString()
            logger.info { "Vedtak skal tilhøre dp-sak " }
            val vedtakFattetHendelse =
                packet.vedtakFattetHendelse(
                    sak =
                        UtsendingSak(
                            id = sakId,
                            kontekst = "Dagpenger",
                        ),
                    behandlingResultat = behandlingResultat,
                )

            sakMediator.merkSakenSomDpSak(vedtakFattetHendelse = vedtakFattetHendelse)
            context.publish(
                key = packet["ident"].asText(),
                message =
                    JsonMessage
                        .newMessage(
                            map =
                                VedtakUtenforArena(
                                    behandlingId = behandlingResultat.behandlingId,
                                    søknadId = behandlingResultat.behandletHendelseId,
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
