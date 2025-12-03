package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
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
                it.requireAny(key = "behandletHendelse.type", values = listOf("Søknad", "Meldekort", "Manuell"))
            }
            validate {
                it.requireKey("ident", "behandlingId", "@opprettet", "behandlingskjedeId")
                it.interestedIn("behandletHendelse", "basertPåBehandling")
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
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val opprettet = packet["@opprettet"].asLocalDateTime()
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
        when (behandletHendelseType) {
            "Søknad" -> {
                val søknadId = packet.søknadId()
                withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
                    logger.info { "Mottok behandling_opprettet hendelse for søknad" }
                    val søknadsbehandlingOpprettetHendelse =
                        SøknadsbehandlingOpprettetHendelse(
                            søknadId = søknadId,
                            behandlingId = behandlingId,
                            ident = ident,
                            opprettet = opprettet,
                            basertPåBehandling = basertPåBehandling,
                            behandlingskjedeId = behandlingskjedeId,
                        )
                    if (basertPåBehandling != null) {
                        sakMediator.knyttTilSak(søknadsbehandlingOpprettetHendelse)
                    } else {
                        sakMediator.opprettSak(søknadsbehandlingOpprettetHendelse)
                    }
                }
            }

            "Meldekort" -> {
                val meldekortId = packet.meldekortId()
                withLoggingContext("meldekortId" to meldekortId, "behandlingId" to "$behandlingId") {
                    logger.info { "Mottok behandling_opprettet hendelse for meldekort" }
                    if (basertPåBehandling != null) {
                        sakMediator.knyttTilSak(
                            MeldekortbehandlingOpprettetHendelse(
                                meldekortId = meldekortId,
                                behandlingId = behandlingId,
                                ident = ident,
                                opprettet = opprettet,
                                basertPåBehandling = basertPåBehandling,
                                behandlingskjedeId = behandlingskjedeId,
                            ),
                        )
                    } else {
                        logger.warn { "Mottok behandling_opprettet av type meldekort, uten 'basertPåBehandling'. Opprettes ikke!" }
                    }
                }
            }

            "Manuell" -> {
                val manuellId = packet.manuellId()
                withLoggingContext("manuellId" to "$manuellId", "behandlingId" to "$behandlingId") {
                    logger.info { "Mottok behandling_opprettet hendelse for manuell behandling" }
                    if (basertPåBehandling != null) {
                        sakMediator.knyttTilSak(
                            ManuellBehandlingOpprettetHendelse(
                                manuellId = manuellId,
                                behandlingId = behandlingId,
                                ident = ident,
                                opprettet = opprettet,
                                basertPåBehandling = basertPåBehandling,
                                behandlingskjedeId = behandlingskjedeId,
                            ),
                        )
                    } else {
                        logger.warn { "Mottok behandling_opprettet av type manuell, uten 'basertPåBehandling'. Opprettes ikke!" }
                    }
                }
            }

            else -> {
                logger.error { "Mottok behandling opprettet for ukjent hendelsetype. Se sikker logg for detaljer." }
                sikkerlogger.error { "Mottok behandling opprettet for ukjent hendelsetype. ${packet.toJson()}" }
            }
        }
    }
}

private fun JsonMessage.søknadId(): UUID = this["behandletHendelse"]["id"].asUUID()

private fun JsonMessage.manuellId(): UUID = this["behandletHendelse"]["id"].asUUID()

private fun JsonMessage.meldekortId(): String = this["behandletHendelse"]["id"].asText()
