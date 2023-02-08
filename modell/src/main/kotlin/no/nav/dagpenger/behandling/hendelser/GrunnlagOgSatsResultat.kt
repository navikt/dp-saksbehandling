package no.nav.dagpenger.behandling.hendelser

import java.math.BigDecimal
import java.util.UUID

class GrunnlagOgSatsResultat(ident: String, behandlingsId: UUID, val grunnlag: BigDecimal, val dagsats: BigDecimal) :
    BehandlingResultatHendelse(ident = ident, behandlingsId = behandlingsId)
