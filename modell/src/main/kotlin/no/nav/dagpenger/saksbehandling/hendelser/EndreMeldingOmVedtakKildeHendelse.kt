package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.MeldingOmVedtakKilde
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class EndreMeldingOmVedtakKildeHendelse(
    val oppgaveId: UUID,
    val meldingOmVedtakKilde: MeldingOmVedtakKilde,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
