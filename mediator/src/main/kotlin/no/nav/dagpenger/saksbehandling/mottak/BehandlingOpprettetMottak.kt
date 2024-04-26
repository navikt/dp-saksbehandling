package no.nav.dagpenger.saksbehandling.mottak

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

internal class BehandlingOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
    private val skjermingKlient: SkjermingKlient,
    private val pdlKlient: PDLKlient,
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
    ) {
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val opprettet = packet["@opprettet"].asLocalDateTime()

        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandling opprettet hendelse for søknadId $søknadId og behandlingId $behandlingId" }

            val erBeskyttetPerson =
                runBlocking {
                    val erSkjermetPerson =
                        async {
                            skjermingKlient.erSkjermetPerson(ident).getOrThrow()
                        }

                    val erAdressebeskyttetPerson =
                        async {
                            pdlKlient.erAdressebeskyttet(ident).getOrThrow()
                        }

                    erSkjermetPerson.await() || erAdressebeskyttetPerson.await()
                }

            if (!erBeskyttetPerson) {
                oppgaveMediator.opprettOppgaveForBehandling(
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                        ident = ident,
                        opprettet = ZonedDateTime.of(opprettet, ZoneId.systemDefault()),
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
            }
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        logger.error { "Forstod ikke behandling opprettet hendelse. \n $problems" }
    }
}
