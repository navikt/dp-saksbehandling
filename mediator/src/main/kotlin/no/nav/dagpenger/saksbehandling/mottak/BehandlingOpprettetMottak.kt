package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
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
                it.requireAny(key = "behandletHendelse.type", values = listOf("Søknad", "Meldekort"))
            }
            validate {
                it.requireKey("ident", "behandlingId", "@opprettet")
                it.interestedIn("behandletHendelse", "basertPåBehandlinger")
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
        val behandlingType = packet["behandletHendelse"]["type"].asText()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val opprettet = packet["@opprettet"].asLocalDateTime()

        when (behandlingType) {
            "Søknad" -> {
                val søknadId = packet.søknadId()
                withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
                    logger.info { "Mottok behandling_opprettet hendelse for søknad" }
                    sakMediator.opprettSak(
                        SøknadsbehandlingOpprettetHendelse(
                            søknadId = søknadId,
                            behandlingId = behandlingId,
                            ident = ident,
                            opprettet = opprettet,
                        ),
                    )
                }
            }

            "Meldekort" -> {
                val meldekortId = packet.meldekortId()
                withLoggingContext("meldekortId" to "$meldekortId", "behandlingId" to "$behandlingId") {
                    logger.info { "Mottok behandling_opprettet hendelse for meldekort" }
                    sakMediator.knyttTilSak(
                        MeldekortbehandlingOpprettetHendelse(
                            meldekortId = meldekortId,
                            behandlingId = behandlingId,
                            ident = ident,
                            opprettet = opprettet,
                            basertPåBehandlinger =
                                packet.basertPåBehandlinger(),
                        ),
                    )
                }
            }
        }
    }
}

private fun JsonMessage.søknadId(): UUID = this["behandletHendelse"]["id"].asUUID()

private fun JsonMessage.basertPåBehandlinger(): List<UUID> = this["basertPåBehandlinger"].map { it.asUUID() }

private fun JsonMessage.meldekortId(): Long = this["behandletHendelse"]["id"].asLong()
