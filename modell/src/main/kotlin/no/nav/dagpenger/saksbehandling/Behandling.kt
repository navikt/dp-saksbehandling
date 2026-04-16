package no.nav.dagpenger.saksbehandling

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val opprettet: LocalDateTime,
    val hendelse: Hendelse,
    val utløstAv: UtløstAvType,
    val oppgaveId: UUID? = null,
) {
    companion object {
        fun rehydrer(
            behandlingId: UUID,
            opprettet: LocalDateTime,
            hendelse: Hendelse,
            utløstAv: UtløstAvType,
        ) = Behandling(
            behandlingId = behandlingId,
            opprettet = opprettet,
            hendelse = hendelse,
            utløstAv = utløstAv,
        )
    }
}

enum class UtløstAvType(
    val applikasjon: Applikasjon,
    @get:JsonValue val rapidNavn: String,
) {
    SØKNAD(applikasjon = Applikasjon.DpBehandling, rapidNavn = "Søknad"),
    MELDEKORT(applikasjon = Applikasjon.DpBehandling, rapidNavn = "Meldekort"),
    MANUELL(applikasjon = Applikasjon.DpBehandling, rapidNavn = "Manuell"),
    REVURDERING(applikasjon = Applikasjon.DpBehandling, rapidNavn = "Omgjøring"),
    INNSENDING(applikasjon = Applikasjon.DpSaksbehandling, rapidNavn = "Innsending"),
    KLAGE(applikasjon = Applikasjon.DpSaksbehandling, rapidNavn = "Klage"),
    ;

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraNavn(navn: String): UtløstAvType =
            entries.firstOrNull { it.rapidNavn == navn }
                ?: throw IllegalArgumentException("Ukjent UtløstAvType: '$navn'. Gyldige verdier: ${entries.map { it.rapidNavn }}")
    }
}
