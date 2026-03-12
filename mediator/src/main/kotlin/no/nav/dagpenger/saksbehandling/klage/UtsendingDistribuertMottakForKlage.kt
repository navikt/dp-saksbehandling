package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.utsending.UtsendingType

internal class UtsendingDistribuertMottakForKlage(
    rapidsConnection: RapidsConnection,
    private val klageMediator: KlageMediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "utsending_distribuert")
                it.requireValue("type", UtsendingType.KLAGEMELDING.name)
            }
            validate { it.requireKey("behandlingId", "ident", "utsendingId", "journalpostId", "distribusjonId") }
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
        val behandlingId = packet["behandlingId"].asUUID()
        val distribusjonId = packet["distribusjonId"].asText()
        val journalpostId = packet["journalpostId"].asText()
        val utsendingId = packet["utsendingId"].asUUID()
        val ident = packet["ident"].asText()

        logger.info {
            "Mottatt distribusjon for klagebehandling $behandlingId med distribusjonsId $distribusjonId og journalpostId $journalpostId"
        }

        if (behandlingId.toString() in setOf("019938f9-23cf-7abe-b3de-797c59c5df0f")) {
            logger.warn { "Skipper behandlingId: $behandlingId fra UtsendingDistribuertMottakForKlage" }
            return
        }
        withLoggingContext(
            "behandlingId" to behandlingId.toString(),
            "distribusjonId" to distribusjonId,
            "journalpostId" to journalpostId,
        ) {
            val utsendingDistribuertHendelse =
                UtsendingDistribuert(
                    behandlingId = behandlingId,
                    utsendingId = utsendingId,
                    ident = ident,
                    journalpostId = journalpostId,
                    distribusjonId = distribusjonId,
                )
            klageMediator.vedtakDistribuert(utsendingDistribuertHendelse)
        }
    }
}
