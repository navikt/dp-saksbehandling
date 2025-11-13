package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.RettTilDagpenger
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class EndreMeldingOmVedtakKildeHendelse(
    val oppgaveId: UUID,
    val meldingOmVedtakKilde: RettTilDagpenger.MeldingOmVedtakKilde,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
