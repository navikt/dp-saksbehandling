package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Sak
import java.util.UUID

data class VedtakFattetHendelse(
    val behandlingId: UUID,
    val søknadId: UUID,
    val ident: String,
    val sak: Sak,
    override val utførtAv: String = "dp-behandling",
) : Hendelse(utførtAv)
