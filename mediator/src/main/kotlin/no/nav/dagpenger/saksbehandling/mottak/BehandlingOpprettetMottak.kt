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
import no.nav.dagpenger.saksbehandling.serder.asUUID
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
        val behandletHendelseType = packet["behandletHendelse"]["type"].stringValue()
        val behandlingId = packet["behandlingId"].asUUID()
        withLoggingContext("behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling_opprettet hendelse av type $behandletHendelseType for behandlingId $behandlingId" }

            val behandletHendelseSkjedde = packet["behandletHendelse"]["skjedde"].asLocalDate()
            val ident = packet["ident"].stringValue()
            val behandlingskjedeId = packet["behandlingskjedeId"].asUUID()
            val basertPåBehandling: UUID? =
                if (packet["basertPåBehandling"].isMissingOrNull()) {
                    null
                } else {
                    packet["basertPåBehandling"].asUUID()
                }
            val skipBehandlingSet =
                setOf<UUID>(
                    UUID.fromString("01a1820d-7251-7495-bc3f-83b3817c326c"),
                    UUID.fromString("01a186c5-2a13-70f5-9755-cb15744f843e"),
                    UUID.fromString("01a18718-a1f2-7503-8e38-d2c55f4333ad"),
                    UUID.fromString("01a18725-3b54-768d-b013-367ecf099fca"),
                )
            if (behandlingId in skipBehandlingSet) {
                logger.info { "Skipper behandlingId: $behandlingId fra BehandlingOpprettetMottak" }
                return
            }

            val utløstAv = DpBehandling.fraBehandletHendelseType(behandletHendelseType)

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
                    sakMediator.opprettEllerKnyttTilSak(hendelse)
                }

                else -> {
                    sakMediator.opprettEllerKnyttTilSak(
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
