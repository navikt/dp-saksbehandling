package no.nav.dagpenger.saksbehandling.utsending.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator

private val logger = KotlinLogging.logger {}

internal class VedtakFattetMottakForUtsending(
    rapidsConnection: RapidsConnection,
    private val utsendingMediator: UtsendingMediator,
    private val sakRepository: SakRepository,
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
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        logger.info { "VedtakFattetMottakForUtsending - behandlingId: ${packet["behandlingId"].asUUID()}" }
        if (vedtakSkalTilhøreDpSak(packet)) {
            val behandlingId = packet["behandlingId"].asUUID()
            val ident = packet["ident"].asText()
            val sakId = sakRepository.hentSakIdForBehandlingId(behandlingId).toString()
            val automatiskBehandlet = packet["automatisk"].asBoolean()
            val behandletHendelseId = packet["behandletHendelse"]["id"].asText()
            val behandletHendelseType = packet["behandletHendelse"]["type"].asText()

            utsendingMediator.startUtsendingForVedtakFattet(
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
            // TODO publiser event om at vedtak er fattet og skal tilhøre sak i dp-sak
        }
    }

    private fun vedtakSkalTilhøreDpSak(packet: JsonMessage): Boolean {
        val dagpengerInnvilget = packet["fastsatt"]["utfall"].asBoolean()
        logger.info { "VedtakFattetForUtsending med utfall: $dagpengerInnvilget" }
        return dagpengerInnvilget
    }
}
