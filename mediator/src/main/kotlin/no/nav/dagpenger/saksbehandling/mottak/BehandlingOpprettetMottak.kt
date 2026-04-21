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
import no.nav.dagpenger.saksbehandling.HendelseBehandler.DpBehandling
import no.nav.dagpenger.saksbehandling.hendelser.DpBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

internal class BehandlingOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val sakMediator: SakMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {

            precondition {
                it.requireValue("@event_name", "behandling_opprettet")
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
        val behandletHendelseType = packet["behandletHendelse"]["type"].asText()
        val behandletHendelseSkjedde = packet["behandletHendelse"]["skjedde"].asLocalDate()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val behandlingskjedeId = packet["behandlingskjedeId"].asUUID()
        val basertPåBehandling: UUID? =
            if (packet["basertPåBehandling"].isMissingOrNull()) {
                null
            } else {
                packet["basertPåBehandling"].asUUID()
            }

        val skipSet = setOf<UUID>(UUID.fromString("019a5e65-869d-78f2-84b9-fdd152f4f9aa"))
        if (behandlingId in skipSet) {
            logger.info { "Skipper behandlingId: $behandlingId fra BehandlingOpprettetMottak" }
            return
        }

        val utløstAv = DpBehandling.fraBehandletHendelseType(behandletHendelseType)

        withLoggingContext("behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling_opprettet hendelse av type $behandletHendelseType (utløstAv=$utløstAv)" }

            when (utløstAv) {
                is DpBehandling.Søknad -> {
                    val søknadId = packet.søknadId()
                    val hendelse =
                        SøknadsbehandlingOpprettetHendelse(
                            søknadId = søknadId,
                            behandlingId = behandlingId,
                            ident = ident,
                            opprettet = behandletHendelseSkjedde.atStartOfDay(),
                            basertPåBehandling = basertPåBehandling,
                            behandlingskjedeId = behandlingskjedeId,
                        )
                    if (basertPåBehandling == null) {
                        withLoggingContext("søknadId" to "$søknadId") {
                            sakMediator.opprettSak(hendelse)
                        }
                    } else {
                        sakMediator.knyttTilSak(hendelse)
                    }
                }

                else -> {
                    sakMediator.knyttTilSak(
                        DpBehandlingOpprettetHendelse(
                            behandlingId = behandlingId,
                            ident = ident,
                            opprettet = behandletHendelseSkjedde.atStartOfDay(),
                            basertPåBehandling = basertPåBehandling,
                            behandlingskjedeId = behandlingskjedeId,
                            type = utløstAv,
                            eksternId = packet.eksternId(),
                        ),
                    )
                }
            }
        }
    }
}

private fun JsonMessage.søknadId(): UUID = this["behandletHendelse"]["id"].asUUID()

private fun JsonMessage.eksternId(): String = this["behandletHendelse"]["id"].asText()
