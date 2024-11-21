package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection

object OppgaveAlertManager {
    sealed interface AlertType {
        val feilMelding: String
        val type: String
    }

    enum class OppgaveAlertType : AlertType {
        OPPGAVE_IKKE_FUNNET {
            override val feilMelding = "Oppgave ikke funnet"
            override val type: String = name
        },
    }

    fun RapidsConnection.sendAlertTilRapid(
        feilType: AlertType,
        utvidetFeilMelding: String,
    ) {
        this.publish(
            JsonMessage.newMessage(
                eventName = "saksbehandling_alert",
                mapOf(
                    "alertType" to feilType.type,
                    "feilMelding" to feilType.feilMelding,
                    "utvidetFeilMelding" to utvidetFeilMelding,
                ),
            ).toJson(),
        )
    }
}
