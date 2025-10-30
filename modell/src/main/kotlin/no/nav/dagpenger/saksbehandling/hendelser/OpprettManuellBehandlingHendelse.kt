package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.time.LocalDateTime
import java.util.UUID

data class OpprettManuellBehandlingHendelse(
    val manuellId: UUID,
    val behandlingId: UUID,
    val ident: String,
    val opprettet: LocalDateTime,
    val basertPåBehandling: UUID,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
