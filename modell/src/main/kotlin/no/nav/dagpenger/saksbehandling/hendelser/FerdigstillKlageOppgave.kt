package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler

class FerdigstillKlageOppgave(override val utførtAv: Saksbehandler) : AnsvarHendelse(
    utførtAv = utførtAv,
    ansvarligIdent = utførtAv.navIdent,
)
