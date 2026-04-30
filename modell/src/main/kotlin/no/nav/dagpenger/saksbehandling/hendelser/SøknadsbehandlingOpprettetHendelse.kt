package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.time.LocalDateTime
import java.util.UUID

data class SøknadsbehandlingOpprettetHendelse(
    val søknadId: UUID,
    val behandlingId: UUID,
    val ident: String,
    val opprettet: LocalDateTime,
    val basertPåBehandling: UUID? = null,
    val behandlingskjedeId: UUID? = null,
    override val utførtAv: Applikasjon = Applikasjon.DpBehandling,
) : Hendelse(utførtAv)
