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

internal class BehandlingOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
    private val pdlKlient: PDLKlient,
    private val skjermingKlient: SkjermingKlient,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "behandling_opprettet") }
            validate { it.requireKey("ident", "søknadId", "behandlingId", "@opprettet") }
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
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val opprettet = packet["@opprettet"].asLocalDateTime()

        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling_opprettet hendelse for søknadId $søknadId og behandlingId $behandlingId" }

            val erAdresseBeskyttetPerson =
                runBlocking {
                    pdlKlient.erAdressebeskyttet(ident).getOrThrow()
                }

            val erSkjermetPerson =
                runBlocking {
                    skjermingKlient.erSkjermetPerson(ident).getOrThrow()
                }

            if (!erAdresseBeskyttetPerson && !erSkjermetPerson) {
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
                logger.info { "Publiserte avbryt_behandling hendelse for behandlingId $behandlingId og søknadId $søknadId" }
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
