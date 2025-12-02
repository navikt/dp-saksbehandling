package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.innsending.InnsendingMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class SøknadBehandlingOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val innsendingMediator: InnsendingMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {

            precondition {
                it.requireValue("@event_name", "behandling_opprettet")
                it.requireValue("behandletHendelse.type", "Søknad")
            }
            validate {
                it.requireKey("ident", "behandlingId", "@opprettet", "behandletHendelse")
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
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val søknadId = packet.søknadId()
        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling_opprettet hendelse for søknad i SøknadBehandlingOpprettetMottak" }
            innsendingMediator.automatiskFerdigstill(
                hendelse =
                    BehandlingOpprettetForSøknadHendelse(
                        ident = ident,
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                    ),
            )
        }
    }
}

private fun JsonMessage.søknadId(): UUID = this["behandletHendelse"]["id"].asUUID()
