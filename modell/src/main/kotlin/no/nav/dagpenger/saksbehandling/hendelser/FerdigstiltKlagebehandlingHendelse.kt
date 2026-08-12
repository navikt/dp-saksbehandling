package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import java.util.UUID

data class FerdigstiltKlagebehandlingHendelse(
    val oppgaveId: UUID,
    val utfall: UtfallType,
    val meldingOmVedtakKilde: Oppgave.MeldingOmVedtakKilde = DP_SAK,
    val meldingOmVedtak: String? = null,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
