package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import java.util.UUID

class StønadsperiodeResultat(ident: String, behandlingsId: UUID, val stønadsperiode: Stønadsperiode) :
    BehandlingResultatHendelse(ident = ident, behandlingsId = behandlingsId)
