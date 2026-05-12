package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import java.util.UUID

internal class SøknadsavklaringLøsningMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val behov =
            listOf(
                "EØSArbeid",
                "BostedslandErNorge",
                "PermittertGrensearbeider",
                "Sanksjon",
                "BarnOver16",
                "PlanleggerUtdanning",
                "EØSPengestøtte",
            )
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAllOrAny(key = "@behov", values = behov)
                it.requireValue("@final", true)
            }
            validate { it.requireKey("@løsning") }
            validate { it.requireKey("oppgaveId") }
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
        val oppgaveId = UUID.fromString(packet["oppgaveId"].asString())

        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
            val løsning = packet["@løsning"]
            val emneknagger = mutableSetOf<String>()

            if (løsning["EØSArbeid"]?.get("verdi")?.asBoolean() == true) {
                emneknagger.add("EØS-inntekt")
            }
            if (løsning["BostedslandErNorge"]?.get("verdi")?.asBoolean() == false) {
                emneknagger.add("Bosatt utland")
            }
            if (løsning["PermittertGrensearbeider"]?.get("verdi")?.asBoolean() == true) {
                emneknagger.add("Grensearbeider")
            }
            if (løsning["Sanksjon"]?.get("verdi")?.asBoolean() == true) {
                emneknagger.add("Mulig sanksjon")
            }
            if (løsning["BarnOver16"]?.get("verdi")?.asBoolean() == true) {
                emneknagger.add("Barn over 16")
            }
            if (løsning["PlanleggerUtdanning"]?.get("verdi")?.asBoolean() == true) {
                emneknagger.add("Utdanning")
            }
            if (løsning["EØSPengestøtte"]?.get("verdi")?.asBoolean() == true) {
                emneknagger.add("EØS-pengestøtte")
            }

            if (emneknagger.isNotEmpty()) {
                logger.info { "Legger til søknadsavklaring-emneknagger: $emneknagger" }
                oppgaveMediator.leggTilEmneknagger(oppgaveId, emneknagger)
            } else {
                logger.info { "Ingen søknadsavklaring-emneknagger å legge til" }
            }
        }
    }
}
