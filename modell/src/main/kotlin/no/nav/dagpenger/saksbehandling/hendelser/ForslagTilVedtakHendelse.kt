package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør
import java.util.UUID

data class ForslagTilVedtakHendelse(
    val ident: String,
    val søknadId: UUID,
    val behandlingId: UUID,
    val emneknagger: Set<String> = emptySet(),
    private val aktør: Aktør,
) : Hendelse(aktør)
