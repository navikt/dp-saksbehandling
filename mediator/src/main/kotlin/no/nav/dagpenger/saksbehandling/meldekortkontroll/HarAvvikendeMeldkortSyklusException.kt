package no.nav.dagpenger.saksbehandling.meldekortkontroll

import java.util.UUID

class HarAvvikendeMeldkortSyklusException(
    behandlingId: UUID,
) : RuntimeException("Behandling $behandlingId har avvikende meldekort syklus")
