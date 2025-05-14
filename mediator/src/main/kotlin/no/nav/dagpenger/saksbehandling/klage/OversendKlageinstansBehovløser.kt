package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.River.PacketListener
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class OversendKlageinstansBehovløser(
    rapidsConnection: RapidsConnection,
    private val klageRepository: KlageRepository,
    private val klageKlient: KlageHttpKlient,
) : PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAll("@behov", listOf("OversendelseKlageinstans"))
                it.forbid("@løsning")
            }
            validate { it.requireKey("ident", "behandlingId", "fagsakId") }
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
        val behandlingId = packet["behandlingId"].asText().let { UUID.fromString(it) }
        val ident = packet["ident"].asText()
        val fagsakId = packet["fagsakId"].asText()
        withLoggingContext("behandlingId" to "$behandlingId") {
            klageRepository.hentKlageBehandling(behandlingId).let { klageBehandling ->
                runBlocking {
                    klageKlient.registrerKlage(
                        klageBehandling = klageBehandling,
                        ident = ident,
                        fagsakId = fagsakId,
                    )
                }.also { resultat ->
                    when (resultat.isSuccess) {
                        true -> {
                            logger.info { "Klage er oversendt til klageinstans $behandlingId" }
                            packet["@løsning"] = mapOf("OversendelseKlageinstans" to "OK")
                        }
                        false -> {
                            logger.info { "Feil ved oversendelse til klageinstans for behandling $behandlingId" }
                            throw RuntimeException("Feil ved oversendelse av klage til klageinstans $behandlingId")
                        }
                    }
                }
            }
//            utsendingMediator.utsendingFinnesForBehandling(behandlingId).let {
//                when (it) {
//                    true -> {
//                        packet["@løsning"] = mapOf("MeldingOmVedtakProdusent" to "Dagpenger")
//                        logger.info { "MeldingOmVedtakProdusent er Dagpenger" }
//                    }
//                    false -> {
//                        packet["@løsning"] = mapOf("MeldingOmVedtakProdusent" to "Arena")
//                        logger.info { "MeldingOmVedtakProdusent er Arena" }
//                    }
//                }
//            }
//            context.publish(key = ident, message = packet.toJson())
        }
    }
}
