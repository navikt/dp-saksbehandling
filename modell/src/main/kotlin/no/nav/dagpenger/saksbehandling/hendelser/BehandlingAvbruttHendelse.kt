package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør
import java.util.UUID

data class BehandlingAvbruttHendelse(
    val behandlingId: UUID,
    val søknadId: UUID,
    val ident: String,
    private val aktør: Aktør = Aktør.System.dpBehandling,
) : Hendelse(aktør)
