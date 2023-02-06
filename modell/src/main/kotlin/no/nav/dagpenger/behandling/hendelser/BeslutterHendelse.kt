package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

class BeslutterHendelse(val beslutterIdent: String, ident: String, behandlingId: UUID) :
    BehandlingResultatHendelse(ident, behandlingId)
