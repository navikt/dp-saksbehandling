package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import java.util.UUID

data class OversendtKlageinstansHendelse(
    val behandlingId: UUID,
    override val utførtAv: Behandler = Applikasjon("dp-behov-send-til-ka"),
) : Hendelse(utførtAv)
