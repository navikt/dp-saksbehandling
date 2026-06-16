package no.nav.dagpenger.saksbehandling.meldekortregister

import java.util.UUID

class BrukerHarEndretMeldesyklusException(
    behandlingId: UUID,
) : RuntimeException("Person med behandling $behandlingId har meldekort med endret meldesyklus")
