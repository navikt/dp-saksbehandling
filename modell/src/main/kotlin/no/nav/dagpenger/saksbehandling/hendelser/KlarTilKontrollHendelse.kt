package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør

class KlarTilKontrollHendelse(utførtAv: Aktør) : AnsvarHendelse(utførtAv = utførtAv, ansvarligIdent = null) {
    // todo ta med OppgaveAnsvar??? Kanskje?
}
