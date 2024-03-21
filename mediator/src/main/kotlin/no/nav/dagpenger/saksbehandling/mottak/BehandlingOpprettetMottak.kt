package no.nav.dagpenger.saksbehandling.mottak

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Mediator
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
    private val mediator: Mediator,
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

            val erBeskyttetPerson = runBlocking {
                val erSkjermetPerson = async {
                    skjermingKlient.erSkjermetPerson(ident)
                        .onFailure { t ->
                            logger.error(t) { "Feil ved oppslag mot skjerming" }
                        }
                        .getOrThrow()
                }

                val erAdressebeskyttetPerson = async {
                    pdlKlient.erAdressebeskyttet(ident)
                        .onFailure { t ->
                            logger.error(t) { "Feil ved oppslag mot pdl" }
                        }
                        .getOrThrow()
                }

                erSkjermetPerson.await() || erAdressebeskyttetPerson.await()
            }

            if (!erBeskyttetPerson) {
                mediator.behandle(
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                        ident = ident,
                        opprettet = ZonedDateTime.of(opprettet, ZoneId.systemDefault()),
                    ),
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

private fun JsonNode.asZonedDateTime() = ZonedDateTime.parse(this.asText())
