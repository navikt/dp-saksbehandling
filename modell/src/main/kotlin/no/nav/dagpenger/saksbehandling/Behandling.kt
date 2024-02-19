package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.VerifiserOpplysningHendelse
import java.util.UUID

class Behandling(
    val behandlingId: UUID,
) {
    var oppgave: Oppgave? = null

    fun håndter(verifiserOpplysningHendelse: VerifiserOpplysningHendelse) {
        if (oppgave == null) {
            oppgave = Oppgave(oppgaveId = UUIDv7.ny(), emneknagger = setOf("VerifiserOpplysninger"))
        }
    }
}
