package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

class BeslutterHendelse(val beslutterIdent: String, ident: String, behandlingsId: UUID) :
    BehandlingResultatHendelse(ident, behandlingsId)
