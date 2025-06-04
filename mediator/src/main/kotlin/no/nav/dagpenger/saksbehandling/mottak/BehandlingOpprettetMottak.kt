package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import java.util.UUID

internal class BehandlingOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
    private val pdlKlient: PDLKlient,
    private val skjermingKlient: SkjermingKlient,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {

            precondition {
                it.requireValue("@event_name", "behandling_opprettet")
                it.requireValue("behandletHendelse.type", "Søknad")
            }
            validate {
                it.requireKey("ident", "behandlingId", "@opprettet")
                it.interestedIn("behandletHendelse")
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
        val søknadId = packet.søknadId()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val opprettet = packet["@opprettet"].asLocalDateTime()

        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling_opprettet hendelse" }

            val erAdresseBeskyttetPerson =
                runBlocking {
                    pdlKlient.erAdressebeskyttet(ident).getOrThrow()
                }

            val erSkjermetPerson =
                runBlocking {
                    skjermingKlient.erSkjermetPerson(ident).getOrThrow()
                }

            if (!erAdresseBeskyttetPerson && !erSkjermetPerson) {
                // opprett en ny sak :)
                oppgaveMediator.opprettOppgaveForBehandling(
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                        ident = ident,
                        opprettet = opprettet,
                    ),
                )
            } else {
                context.publish(
                    key = ident,
                    message =
                        JsonMessage.newMessage(
                            eventName = "avbryt_behandling",
                            map =
                                mapOf(
                                    "behandlingId" to behandlingId,
                                    "søknadId" to søknadId,
                                    "ident" to ident,
                                ),
                        ).toJson(),
                )
                logger.info { "Publiserte avbryt_behandling hendelse" }
            }
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logger.error { "Forstod ikke behandling_opprettet hendelse. \n $problems" }
    }
}

private fun JsonMessage.søknadId(): UUID = this["behandletHendelse"]["id"].asUUID()
