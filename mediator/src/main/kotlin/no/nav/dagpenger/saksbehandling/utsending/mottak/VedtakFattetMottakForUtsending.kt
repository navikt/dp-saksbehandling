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

            logger.info { "VedtakFattetMottakForUtsending - publiserer behov for utsending" }
            context.publish(
                JsonMessage.newMessage(
                    mapOf(
                        "@event_name" to "behov",
                        "@behov" to "StartUtsending",
                        "behandlingId" to behandlingId.toString(),
                        "ident" to ident,
                    ),
                ).toJson(),
            )

            val vedtakUtenforArena =
                VedtakUtenforArena(
                    behandlingId = behandlingId.toString(),
                    søknadId = behandletHendelseId,
                    ident = ident,
                    sakId = sakId,
                )
            // TODO publiser event om at vedtak er fattet og skal tilhøre sak i dp-sak
            context.publish(
                JsonMessage
                    .newMessage(
                        map = vedtakUtenforArena.toMap(),
                    ).toJson(),
            )
        }
    }

    private fun vedtakSkalTilhøreDpSak(packet: JsonMessage): Boolean {
        val dagpengerInnvilget = packet["fastsatt"]["utfall"].asBoolean()
        logger.info { "VedtakFattetForUtsending med utfall: $dagpengerInnvilget" }
        return dagpengerInnvilget
    }
}

private data class VedtakUtenforArena(
    val behandlingId: String,
    val søknadId: String,
    val ident: String,
    val sakId: String,
) {
    fun toMap() =
        mapOf(
            "@event_name" to "vedtak_fattet_utenfor_arena",
            "behandlingId" to behandlingId,
            "søknadId" to søknadId,
            "ident" to ident,
            "sakId" to sakId,
        )
}
