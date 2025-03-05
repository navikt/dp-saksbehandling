package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.util.UUID

data class PåVentFristUtgåttHendelse(
    val oppgaveId: UUID,
    override val utførtAv: Applikasjon = Applikasjon("dp-saksbehandling"),
) : Hendelse(utførtAv)
