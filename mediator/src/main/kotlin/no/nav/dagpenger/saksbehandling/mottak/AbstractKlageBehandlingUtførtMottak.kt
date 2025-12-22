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
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal abstract class AbstractKlageBehandlingUtførtMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        private const val KLAGE_BEHANDLING_UTFØRT_EVENT_NAME = "klage_behandling_utført"

        internal fun JsonNode.asSaksbehandler(): Saksbehandler {
            // todo: mer validering på noder eksister og sånt
            return Saksbehandler(
                navIdent = this["navIdent"].asText(),
                grupper = this["grupper"].map { it.asText() }.toSet(),
                tilganger = this["tilganger"].map { TilgangType.valueOf(it.asText()) }.toSet(),
            )
        }
    }

    init {
        logger.info { "Starter $mottakNavn" }
        River(rapidsConnection).apply(rapidFilter()).register(this)
    }

    fun rapidFilter(): River.() -> Unit =
        {
            precondition {
                it.requireValue("@event_name", KLAGE_BEHANDLING_UTFØRT_EVENT_NAME)
                it.requireKey("ident", "behandlingId", "utfall", "saksbehandler")
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
            logger.info { "Mottatt klage_behandling_utført for behandlingId=$behandlingId med utfall: $utfall" }
            håndter(
                behandlingId = behandlingId,
                utfall = utfall,
                ident = ident,
                saksbehandler = saksbehandler,
            )
        }
    }
}

internal class KlageBehandlingUtførtMottakForOppgave(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : AbstractKlageBehandlingUtførtMottak(rapidsConnection) {
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
