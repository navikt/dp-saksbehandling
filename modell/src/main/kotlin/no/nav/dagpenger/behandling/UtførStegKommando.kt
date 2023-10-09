package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.PersonHendelse
import java.time.LocalDateTime
import java.util.UUID

class UtførStegKommando(
    oppgaveUUID: UUID,
    saksbehandler: Saksbehandler,
    begrunnelse: String,
    val ident: String,
    val token: String,
    private val block: Behandling.(sporing: Sporing) -> Unit,
) : Kommando(oppgaveUUID, saksbehandler, begrunnelse, ident) {
    override fun utfør(behandling: Behandling) {
        behandling.utfør(this)
    }

    fun besvar(behandling: Behandling) {
        behandling.block(sporing())
    }
}

abstract class Kommando(
    val oppgaveUUID: UUID,
    val saksbehandler: Saksbehandler,
    private val begrunnelse: String,
    ident: String,
    private val utført: LocalDateTime = LocalDateTime.now(),
) : PersonHendelse(UUID.randomUUID(), ident) {
    fun sporing(): ManuellSporing {
        return ManuellSporing(utført, saksbehandler, begrunnelse)
    }

    abstract fun utfør(behandling: Behandling)
}
