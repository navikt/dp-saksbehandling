package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør
import java.util.UUID

class KlarTilKontrollHendelse(oppgaveId: UUID, utførtAv: Aktør) : AnsvarHendelse(utførtAv = utførtAv, ansvarligIdent = null) {
    // todo ta med OppgaveAnsvar??? Kanskje?
}
