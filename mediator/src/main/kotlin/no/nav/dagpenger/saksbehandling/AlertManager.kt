package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

object AlertManager {
    sealed interface AlertType {
        val feilMelding: String
        val type: String
    }

    data class UtsendingIkkeFullført(
        val utsendingId: UUID,
        val tilstand: String,
        val sistEndret: LocalDateTime,
        val oppgaveId: UUID,
        val behandlingId: UUID,
        val personId: UUID,
    ) : AlertType {
        override val feilMelding by lazy {
            """
            Utsending ikke fullført for $utsendingId.
            Den har vært i tilstand $tilstand i ${timerSiden()} timer (sist endret: $sistEndret)
            OppgaveId: $oppgaveId
            BehandlingId: $behandlingId
            PersonId: $personId
            """.trimIndent()
        }

        private fun timerSiden(): String = ChronoUnit.HOURS.between(sistEndret, LocalDateTime.now()).toString()

        override val type: String = "UTSENDING_IKKE_FULLFØRT"
    }

    data class OversendKlageinstansIkkeFullført(
        val behandlingId: UUID,
        val tilstand: String,
        val sistEndret: LocalDateTime,
    ) : AlertType {
        override val feilMelding =
            "Oversendelse til klageinstans ikke fullført for klagebehandling $behandlingId. " +
                "Den har vært i tilstand $tilstand siden $sistEndret"
        override val type: String = "OVERSEND_KLAGEINSTANS_IKKE_FULLFØRT"
    }

    enum class OppgaveAlertType : AlertType {
        OPPGAVE_IKKE_FUNNET {
            override val feilMelding = "Oppgave ikke funnet"
            override val type: String = name
        },
        BEHANDLING_IKKE_FUNNET {
            override val feilMelding = "Behandling ikke funnet"
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
