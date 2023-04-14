package no.nav.dagpenger.behandling

import java.util.UUID

class SøknadBehandlet(
    internal val behandlingId: UUID,
    internal val innvilget: Boolean,
)
