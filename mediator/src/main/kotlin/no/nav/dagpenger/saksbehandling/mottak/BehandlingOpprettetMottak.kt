package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.hendelser.GenerellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class BehandlingOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val sakMediator: SakMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behandling_opprettet")
                it.requireAny(
                    key = "behandletHendelse.type",
                    values = UtløstAvType.entries.map { t -> t.rapidNavn },
                )
            }
            validate {
                it.requireKey("ident", "behandlingId", "behandletHendelse", "behandlingskjedeId")
                it.interestedIn("basertPåBehandling")
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

        val skipSet = setOf<UUID>(UUID.fromString("019a5e65-869d-78f2-84b9-fdd152f4f9aa"))
        if (behandlingId in skipSet) {
            logger.info { "Skipper behandlingId: $behandlingId fra BehandlingOpprettetMottak" }
            return
        }

        val type = UtløstAvType.fraNavn(packet["behandletHendelse"]["type"].asText())
        val behandletHendelseId = packet["behandletHendelse"]["id"].asText()
        val behandletHendelseSkjedde = packet["behandletHendelse"]["skjedde"].asLocalDate()
        val ident = packet["ident"].asText()
        val behandlingskjedeId = packet["behandlingskjedeId"].asUUID()
        val basertPåBehandling: UUID? =
            if (packet["basertPåBehandling"].isMissingOrNull()) {
                null
            } else {
                packet["basertPåBehandling"].asUUID()
            }

        withLoggingContext("behandlingId" to "$behandlingId", "type" to type.rapidNavn) {
            logger.info { "Mottok behandling_opprettet hendelse for ${type.rapidNavn}" }

            if (type == UtløstAvType.REVURDERING && basertPåBehandling == null) {
                logger.warn { "Mottok behandling_opprettet av type ${type.rapidNavn}, uten 'basertPåBehandling'. Opprettes ikke!" }
                return
            }

            val hendelse =
                GenerellBehandlingOpprettetHendelse(
                    behandlingId = behandlingId,
                    ident = ident,
                    opprettet = behandletHendelseSkjedde.atStartOfDay(),
                    type = type,
                    behandletHendelseId = behandletHendelseId,
                    basertPåBehandling = basertPåBehandling,
                    behandlingskjedeId = behandlingskjedeId,
                )

            sakMediator.opprettEllerKnyttTilSak(hendelse)
        }
    }
}
