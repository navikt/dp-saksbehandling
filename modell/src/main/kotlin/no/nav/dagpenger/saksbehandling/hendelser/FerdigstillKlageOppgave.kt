package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

class FerdigstillKlageOppgave(val behandlingId: UUID, override val utførtAv: Saksbehandler) : AnsvarHendelse(
    utførtAv = utførtAv,
    ansvarligIdent = utførtAv.navIdent,
)
