package no.nav.dagpenger.saksbehandling.sak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
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
        logger.info { "BehandlingsresultatMottakForSak basert på $behandlingsresultat" }
        val behandlingId = UUID.fromString(packet["behandlingId"].stringValue())
        withLoggingContext("behandlingId" to "$behandlingId") {
            val ident = packet["ident"].stringValue()

            if (behandlingsresultat.vedtakFattetINySak()) {
                val sakId = sakRepository.hentSakIdForBehandlingId(behandlingsresultat.behandlingId).toString()
                logger.info { "Vedtak fattet i ny sak" }
                val vedtakFattetHendelse =
                    packet.vedtakFattetHendelse(
                        sak =
                            UtsendingSak(
                                id = sakId,
                                kontekst = "Dagpenger",
                            ),
                        behandlingsresultat = behandlingsresultat,
                    )

                val erFørsteDpSak = (sakRepository.finnSisteDagpengeSakId(ident) == null)
                if (erFørsteDpSak) {
                    sakMediator.merkSakenSomDpSak(vedtakFattetHendelse = vedtakFattetHendelse)
                }

                context.publish(
                    key = ident,
                    message =
                        JsonMessage
                            .newMessage(
                                map =
                                    NyDagpengesak(
                                        erFørsteDpSak = erFørsteDpSak,
                                        behandlingId = behandlingId,
                                        // Forutsetter: override fun requiredBehandletHendelseType(): List<String> = listOf("Søknad")
                                        søknadId = behandlingsresultat.behandletHendelseId,
                                        ident = ident,
                                        sakId = sakId,
                                        førteTil = behandlingsresultat.førteTil,
                                    ).toMap(),
                            ).toJson(),
                )
            }
        }
    }

    private data class NyDagpengesak(
        val erFørsteDpSak: Boolean,
        val behandlingId: UUID,
        val søknadId: String,
        val ident: String,
        val sakId: String,
        val førteTil: String,
    ) {
        fun toMap(): Map<String, String> {
            val eventName =
                if (erFørsteDpSak) {
                    "vedtak_fattet_utenfor_arena"
                } else {
                    "vedtak_fattet_i_ny_sak"
                }
            return mapOf(
                "@event_name" to eventName,
                "behandlingId" to behandlingId.toString(),
                "søknadId" to søknadId,
                "ident" to ident,
                "sakId" to sakId,
                "førteTil" to førteTil,
            )
        }
    }
}
