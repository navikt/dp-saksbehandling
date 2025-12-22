package no.nav.dagpenger.saksbehandling.mottak

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal abstract class KlageBehandlingUtførtMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        private const val KLAGE_BEHANDLING_UTFØRT_EVENT_NAME = "klage_behandling_utført"
    }

    init {
        logger.info { "Starter $mottakNavn" }
        River(rapidsConnection).apply(rapidFilter()).register(this)
    }

    fun rapidFilter(): River.() -> Unit =
        {
            precondition {
                it.requireValue("@event_name", KLAGE_BEHANDLING_UTFØRT_EVENT_NAME)
                it.requireKey("ident", "behandlingId", "utfall", "saksbehandlerIdent")
            }
        }

    protected abstract val mottakNavn: String

    protected abstract fun håndter(
        behandlingId: UUID,
        utfall: UtfallType,
        ident: String,
        saksbehandler: Saksbehandler,
    )

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val utfall = UtfallType.valueOf(packet["utfall"].asText())
        val saksbehandler: Saksbehandler = packet["saksbehandler"].asSaksbehandler()
        withLoggingContext(
            "behandlingId" to behandlingId.toString(),
        ) {
            logger.info { "Mottatt klage_behandling_utført for behandlingId=$behandlingId" }
            håndter(
                behandlingId = behandlingId,
                utfall = utfall,
                ident = ident,
                saksbehandler = saksbehandler,
            )
        }
    }

    private fun JsonNode.asSaksbehandler(): Saksbehandler {
        // todo: hente grupper og tilganger fra pakken
        return Saksbehandler(
            navIdent = this["saksbehandlerIdent"].asText(),
            grupper = emptySet(),
            tilganger = emptySet(),
        )
    }
}

internal class KlageBehandlingUtførtMottakForOppgave(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : KlageBehandlingUtførtMottak(rapidsConnection) {
    override val mottakNavn: String = "KlageBehandlingUtførtMottakForOppgave"

    override fun håndter(
        behandlingId: UUID,
        utfall: UtfallType,
        ident: String,
        saksbehandler: Saksbehandler,
    ) {
        oppgaveMediator.ferdigstillOppgave(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        )
    }
}
