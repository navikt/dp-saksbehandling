package no.nav.dagpenger.saksbehandling.sak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.mottak.asUUID

private val logger = KotlinLogging.logger {}

internal class VedtakFattetMottakForSak(
    rapidsConnection: RapidsConnection,
    private val sakRepository: SakRepository,
    private val sakMediator: SakMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "vedtak_fattet")
                it.requireValue("behandletHendelse.type", "Søknad")
                it.requireKey("fastsatt")
            }
            validate {
                it.requireKey("ident", "behandlingId", "behandletHendelse", "automatisk")
            }
        }
    }

    init {
        logger.info { "Starter VedtakFattetMottakForSak" }
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asUUID()
        withLoggingContext("behandlingId" to "$behandlingId") {
            logger.info { "Mottok vedtak_fattet hendelse i VedtakFattetMottakForSak" }

            val skipSet = emptySet<String>()
            if (behandlingId.toString() in skipSet) {
                logger.info { "Skipper behandlingId: $behandlingId fra VedtakFattetMottakForSak" }
                return
            }
            if (vedtakSkalTilhøreDpSak(packet)) {
                val ident = packet["ident"].asText()
                val sakId = sakRepository.hentSakIdForBehandlingId(behandlingId).toString()
                val automatiskBehandlet = packet["automatisk"].asBoolean()
                val behandletHendelseId = packet["behandletHendelse"]["id"].asText()
                val behandletHendelseType = packet["behandletHendelse"]["type"].asText()
                logger.info { "Vedtak skal tilhøre dp-dak " }
                sakMediator.merkSakenSomDpSak(
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = behandletHendelseId,
                        behandletHendelseType = behandletHendelseType,
                        ident = ident,
                        sak =
                            UtsendingSak(
                                id = sakId,
                                kontekst = "Dagpenger",
                            ),
                        automatiskBehandlet = automatiskBehandlet,
                    ),
                )
//                sakRepository.settErDpSakForBehandling(
//                    behandlingId = behandlingId,
//                    erDpSak = true,
//                )
            }
        }
    }

    private fun vedtakSkalTilhøreDpSak(packet: JsonMessage): Boolean {
        val dagpengerInnvilget = packet["fastsatt"]["utfall"].asBoolean()
        logger.info { "VedtakFattetForUtsending med utfall: $dagpengerInnvilget" }
        return dagpengerInnvilget
    }
}
