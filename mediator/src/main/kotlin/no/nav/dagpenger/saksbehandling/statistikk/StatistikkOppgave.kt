package no.nav.dagpenger.saksbehandling.statistikk

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.saksbehandling.Oppgave
import java.time.LocalDateTime
import java.util.UUID

data class StatistikkOppgave constructor(
    val behandling: StatistikkBehandling,
    val sakId: UUID,
    val oppgaveTilstander: List<OppgaveTilstandEndring>,
    val personIdent: String,
) {
    companion object {
        private val objectMapper =
            jacksonObjectMapper().apply {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
    }

    fun toJson(): String = objectMapper.writeValueAsString(this)

    constructor(
        oppgave: Oppgave,
        sakId: UUID,
    ) : this(
        behandling =
            StatistikkBehandling(
                id = oppgave.behandling.behandlingId,
                tidspunkt = oppgave.behandling.opprettet,
                basertPåBehandling = null,
                utløstAv =
                    UtløstAv(
                        type = oppgave.behandling.utløstAv.name,
                        tidspunkt = oppgave.behandling.opprettet, // todo
                    ),
            ),
        sakId = sakId,
        oppgaveTilstander =
            oppgave.tilstandslogg.map {
                OppgaveTilstandEndring(
                    tilstand = it.tilstand.name,
                    tidspunkt = it.tidspunkt,
                    saksbehandlerIdent = null, // todo
                    beslutterIdent = null, // todo
                )
            },
        personIdent = oppgave.personIdent(),
    )

    data class StatistikkBehandling(
        val id: UUID,
        val tidspunkt: LocalDateTime,
        val basertPåBehandling: UUID?,
        val utløstAv: UtløstAv,
    )

    data class UtløstAv(
        val type: String,
        val tidspunkt: LocalDateTime,
    )

    data class OppgaveTilstandEndring(
        val tilstand: String,
        val tidspunkt: LocalDateTime,
        val saksbehandlerIdent: String?,
        val beslutterIdent: String?,
    )
}
