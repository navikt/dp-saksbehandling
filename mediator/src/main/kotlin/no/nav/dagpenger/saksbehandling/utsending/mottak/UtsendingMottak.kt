package no.nav.dagpenger.saksbehandling.utsending.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse

class UtsendingMottak(rapidsConnection: RapidsConnection, private val utsendingMediator: UtsendingMediator) :
    River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "start_utsending")
            }
            validate { it.requireKey("oppgaveId", "behandlingId", "ident", "sak") }
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
        val oppgaveId = packet["oppgaveId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val sak =
            Sak(
                id = packet["sak"]["id"].asText(),
                kontekst = packet["sak"]["kontekst"].asText(),
            )
        withLoggingContext(
            "oppgaveId" to "$oppgaveId",
            "behandlingId" to "$behandlingId",
            "sak" to sak.id,
        ) {
            utsendingMediator.mottaStartUtsending(
                StartUtsendingHendelse(
                    oppgaveId = oppgaveId,
                    behandlingId = behandlingId,
                    ident = ident,
                    sak = sak,
                ),
            )
        }
    }
}
