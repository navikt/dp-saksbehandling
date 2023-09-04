package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.api.models.NyttSvarDTO
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import java.time.LocalDateTime

class UtførStegKommando(val saksbehandler: Saksbehandler, val svar: NyttSvarDTO): Kommando(saksbehandler) {
    override fun sporing(): Sporing {
        TODO("Not yet implemented")
    }

    override fun utfør(oppgave: Oppgave) {
        oppgave.behandle(this)
    }

}

abstract class Kommando(saksbehandler: Saksbehandler, utført: LocalDateTime = LocalDateTime.now()) {
    abstract fun sporing(): Sporing
    abstract fun utfør(oppgave: Oppgave)
}
