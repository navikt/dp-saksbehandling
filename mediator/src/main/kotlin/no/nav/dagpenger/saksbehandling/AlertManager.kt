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

    fun LocalDateTime.timserSiden(): String = ChronoUnit.HOURS.between(this, LocalDateTime.now()).toString()

    internal class OppgaveOpprettetTilstandAlert(
        private val oppgaveId: UUID,
        private val sistEndret: LocalDateTime,
        private val utløstAvType: UtløstAvType,
    ) : AlertType {
        override val feilMelding: String
            get() {
                return """
                    OppgaveId: $oppgaveId
                    Utløst av: $utløstAvType
                    Oppgave har vært i tilstand: Opprettet i ${sistEndret.timserSiden()} timer.
                    Sist endret: $sistEndret)
                    """.trimIndent()
            }
        override val type: String = "OPPGAVE_OPPRETTET_TILSTAND_ALERT"
    }

    data class KnytningTilSakFeil(
        val behandlingId: UUID,
        val hendelseType: String,
        val knyttTilSakResultat: KnyttTilSakResultat,
    ) : AlertType {
        override val feilMelding =
            "Knytning til sak feilet for behandlingId: $behandlingId, hendelsesType: $hendelseType og resultat: $knyttTilSakResultat"
        override val type: String = "KNYTNING_TIL_SAK_FEIL"
    }

    data class InnsendingIkkeFerdigstilt(
        val innsendingId: UUID,
        val tilstand: String,
        val sistEndret: LocalDateTime,
        val journalpostId: String,
        val personId: UUID,
    ) : AlertType {
        override val feilMelding by lazy {
            """
            Innsending ikke fullført for innsendingId: $innsendingId.
            Den har vært i tilstand $tilstand i ${timerSiden()} timer (sist endret: $sistEndret)
            JournalpostId: $journalpostId
            PersonId: $personId
            """.trimIndent()
        }

        private fun timerSiden(): String = ChronoUnit.HOURS.between(sistEndret, LocalDateTime.now()).toString()

        override val type: String = "INNSENDING_IKKE_FULLFØRT"
    }

    data class UtsendingIkkeFullført(
        val utsendingId: UUID,
        val tilstand: String,
        val sistEndret: LocalDateTime,
        val behandlingId: UUID,
        val personId: UUID,
    ) : AlertType {
        override val feilMelding by lazy {
            """
            Utsending ikke fullført for utsendingId: $utsendingId.
            Den har vært i tilstand $tilstand i ${timerSiden()} timer (sist endret: $sistEndret)
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
        BEHANDLING_IKKE_FUNNET {
            override val feilMelding = "Behandling ikke funnet"
            override val type: String = name
        },
    }

    fun RapidsConnection.sendAlertTilRapid(
        feilType: AlertType,
        utvidetFeilMelding: String?,
    ) {
        this.publish(
            JsonMessage.newMessage(
                eventName = "saksbehandling_alert",
                mutableMapOf(
                    "alertType" to feilType.type,
                    "feilMelding" to feilType.feilMelding,
                ).also {
                    utvidetFeilMelding?.let { feilMelding ->
                        it["utvidetFeilMelding"] = feilMelding
                    }
                },
            ).toJson(),
        )
    }
}
