package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

class SøknadBehandletHendelse(
    ident: String,
    val behandlingId: UUID,
    val innvilget: Boolean,
) : Hendelse(ident)
