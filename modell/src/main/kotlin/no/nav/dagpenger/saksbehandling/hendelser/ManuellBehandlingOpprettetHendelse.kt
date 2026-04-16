package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.time.LocalDateTime
import java.util.UUID

@Deprecated("Bruk GenerellBehandlingOpprettetHendelse i stedet. Beholdes for deserialisering av eksisterende DB-poster.")
data class ManuellBehandlingOpprettetHendelse(
    val manuellId: UUID,
    val behandlingId: UUID,
    val ident: String,
    val opprettet: LocalDateTime,
    val basertPåBehandling: UUID,
    val behandlingskjedeId: UUID? = null,
    override val utførtAv: Applikasjon = Applikasjon.DpBehandling,
) : Hendelse(utførtAv)
