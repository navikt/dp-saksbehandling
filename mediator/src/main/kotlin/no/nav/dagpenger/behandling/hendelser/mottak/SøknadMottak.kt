package no.nav.dagpenger.behandling.hendelser.mottak

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

internal class SøknadMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
            validate { it.demandAny("type", listOf("NySøknad")) }
            validate { it.requireKey("fødselsnummer", "journalpostId") }
            validate {
                it.require("søknadsData") { data ->
                    data["søknad_uuid"].asUUID()
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val journalpostId = packet["journalpostId"].asText()
        val ident = packet["fødselsnummer"].asText()

        val søknadID = packet["søknadsData"]["søknad_uuid"].asUUID()
        withLoggingContext(
            "søknadId" to søknadID.toString(),
        ) {
            val søknadHendelse = SøknadHendelse(søknadID, journalpostId, ident)
            logger.info { "Fått søknadhendelse for $søknadID" }
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        logger.info { "${this.javaClass.simpleName} kunne ikke lese melding: \n $problems" }
    }

    private fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
}
