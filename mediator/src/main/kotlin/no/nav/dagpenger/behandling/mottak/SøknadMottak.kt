package no.nav.dagpenger.behandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.Mediator
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class SøknadMottak(
    rapidsConnection: RapidsConnection,
    private val mediator: Mediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
            validate { it.demandAny("type", listOf("NySøknad")) }
            validate { it.requireKey("fødselsnummer", "journalpostId", "datoRegistrert") }
            validate {
                it.require("søknadsData") { data ->
                    data["søknad_uuid"].asUUID()
                }
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        val ident = packet["fødselsnummer"].asText()

        val søknadID = packet["søknadsData"]["søknad_uuid"].asUUID()
        withLoggingContext(
            "søknadId" to søknadID.toString(),
        ) {
            val søknadInnsendtHendelse =
                SøknadInnsendtHendelse(
                    søknadId = søknadID,
                    journalpostId = journalpostId,
                    ident = ident,
                    innsendtDato = packet["datoRegistrert"].asLocalDateTime().toLocalDate(),
                )
            mediator.behandle(søknadInnsendtHendelse)
            logger.info { "Fått SøknadInnsendtHendelse for $søknadID" }
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        logger.info { "${this.javaClass.simpleName} kunne ikke lese melding: \n $problems" }
    }
}
