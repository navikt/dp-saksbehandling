package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class EndreMeldingOmVedtakKildeHendelse(
    val oppgaveId: UUID,
    val meldingOmVedtakKilde: Oppgave.MeldingOmVedtakKilde,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
