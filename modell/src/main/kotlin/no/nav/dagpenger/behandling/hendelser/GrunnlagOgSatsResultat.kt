package no.nav.dagpenger.behandling.hendelser

import java.math.BigDecimal
import java.util.UUID

class GrunnlagOgSatsResultat(ident: String, val behandlingId: UUID, val grunnlag: BigDecimal, val dagsats: BigDecimal) : BehandlingResultatHendelse(ident = ident, behandlingId = behandlingId)
