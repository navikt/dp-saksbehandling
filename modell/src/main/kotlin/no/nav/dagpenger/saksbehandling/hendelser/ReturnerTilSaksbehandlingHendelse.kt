package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.ReturnerTilSaksbehandlingÅrsak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class ReturnerTilSaksbehandlingHendelse(
    val oppgaveId: UUID,
    val årsak: ReturnerTilSaksbehandlingÅrsak = ReturnerTilSaksbehandlingÅrsak.ANNET,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
