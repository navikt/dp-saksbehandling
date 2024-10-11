package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class KlarTilKontrollHendelse(
    val oppgaveId: UUID,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
