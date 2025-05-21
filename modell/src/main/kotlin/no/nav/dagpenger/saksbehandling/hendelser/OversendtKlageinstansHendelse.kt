package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.util.UUID

data class OversendtKlageinstansHendelse(
    val behandlingId: UUID,
    override val utførtAv: Applikasjon = Applikasjon("dp-behov-send-til-ka"),
) : Hendelse(utførtAv)
