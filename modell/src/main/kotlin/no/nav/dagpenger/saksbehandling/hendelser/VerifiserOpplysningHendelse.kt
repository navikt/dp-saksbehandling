package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class VerifiserOpplysningHendelse(
    val behandlingId: UUID,
    val ident: String,
)
