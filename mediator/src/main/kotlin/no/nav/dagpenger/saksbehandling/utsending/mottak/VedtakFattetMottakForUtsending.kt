package no.nav.dagpenger.saksbehandling.utsending.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtakInnvilgetHendelse
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class VedtakFattetMottakForUtsending(
    rapidsConnection: RapidsConnection,
    private val utsendingMediator: UtsendingMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "vedtak_fattet")
                it.requireValue("behandletHendelse.type", "Søknad")
                it.requireKey("fastsatt")
            }
            validate {
                it.requireKey("ident", "behandlingId")
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
        val utfall = packet["fastsatt"]["utfall"].asBoolean()

        if (utfall) {
            val behandlingId = packet["behandlingId"].asUUID()
            val ident = packet["ident"].asText()
            utsendingMediator.behandleUtsendingForVedtakFattetIDpSak(
                VedtakInnvilgetHendelse(
                    behandlingId = behandlingId,
                    ident = ident,
                    // todo not in use. Fjerne når vi fjerner oppgaveId fra Utsending
                    oppgaveId = UUID.randomUUID(),
                ),
            )
        }
    }
}
