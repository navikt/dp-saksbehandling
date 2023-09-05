package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import java.time.LocalDateTime
import java.util.UUID

class UtførStegKommando(
    oppgaveUUID: UUID,
    saksbehandler: Saksbehandler,
    begrunnelse: String,
    private val block: Behandling.(sporing: Sporing) -> Unit,
) : Kommando(oppgaveUUID, saksbehandler, begrunnelse) {
    override fun utfør(behandling: Behandling) {
        behandling.utfør(this)
    }

    fun besvar(behandling: Behandling) {
        behandling.block(sporing())
    }
}

abstract class Kommando(
    val oppgaveUUID: UUID,
    private val saksbehandler: Saksbehandler,
    private val begrunnelse: String,
    private val utført: LocalDateTime = LocalDateTime.now(),
) : Hendelse(UUID.randomUUID()) {

    fun sporing(): ManuellSporing {
        return ManuellSporing(utført, saksbehandler, begrunnelse)
    }

    abstract fun utfør(behandling: Behandling)
}
