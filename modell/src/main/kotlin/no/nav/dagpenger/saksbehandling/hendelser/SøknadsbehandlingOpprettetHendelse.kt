package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør
import java.time.LocalDateTime
import java.util.UUID

data class SøknadsbehandlingOpprettetHendelse(
    val søknadId: UUID,
    val behandlingId: UUID,
    val ident: String,
    val opprettet: LocalDateTime,
    private val aktør: Aktør = Aktør.System.dpSaksbehandling,
) : Hendelse(aktør)
