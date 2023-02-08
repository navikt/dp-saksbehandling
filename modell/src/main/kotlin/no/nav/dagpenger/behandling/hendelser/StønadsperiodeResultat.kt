package no.nav.dagpenger.behandling.hendelser

import java.math.BigDecimal
import java.util.UUID

class StønadsperiodeResultat(ident: String, behandlingId: UUID, val stønadsperiode: BigDecimal) :
    BehandlingResultatHendelse(ident = ident, behandlingId = behandlingId)
