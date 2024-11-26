package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import java.time.LocalDateTime
import java.util.UUID

object OppgaveAlertManager {
    sealed interface AlertType {
        val feilMelding: String
        val type: String
    }

    data class UtsendingIkkeFullført(
        val utsendingId: UUID,
        val tilstand: Utsending.Tilstand,
        val sistEndret: LocalDateTime,
    ) : AlertType {
        override val feilMelding =
            "Utsending ikke fullført for $utsendingId. " +
                "Den har vært i tilstand ${tilstand.type.name} siden $sistEndret"
        override val type: String = "UTSENDING_IKKE_FULLFØRT"
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
