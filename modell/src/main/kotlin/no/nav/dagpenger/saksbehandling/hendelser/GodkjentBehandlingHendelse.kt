package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

// TODO: Vil defaultverdi for meldingOmVedtakKilde forhindre deserialiseringstrøbbel for tidligere hendelser?
data class GodkjentBehandlingHendelse(
    val oppgaveId: UUID,
    val meldingOmVedtakKilde: Oppgave.MeldingOmVedtakKilde = DP_SAK,
    val meldingOmVedtak: String? = null,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
