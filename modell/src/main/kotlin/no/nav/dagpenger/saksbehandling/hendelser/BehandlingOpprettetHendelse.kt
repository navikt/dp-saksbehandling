package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.BehandlingType
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingOpprettetHendelse(
    val behandlingId: UUID,
    val ident: String,
    val sakId: UUID,
    val opprettet: LocalDateTime,
    val type: BehandlingType,
    override val utførtAv: Behandler = Applikasjon("dp-mottak"),
) : Hendelse(utførtAv)
