package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør
import java.time.LocalDate
import java.util.UUID

data class UtsettOppgaveHendelse(
    val oppgaveId: UUID,
    val navIdent: String,
    val utsattTil: LocalDate,
    val beholdOppgave: Boolean,
    private val aktør: Aktør,
) : Hendelse(aktør)
