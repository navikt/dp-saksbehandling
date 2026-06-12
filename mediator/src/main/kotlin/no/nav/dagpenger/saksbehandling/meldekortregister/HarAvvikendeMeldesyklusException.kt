package no.nav.dagpenger.saksbehandling.meldekortregister

import java.util.UUID

class HarAvvikendeMeldesyklusException(
    behandlingId: UUID,
) : RuntimeException("Person med behandling $behandlingId har avvikende meldesyklus")
