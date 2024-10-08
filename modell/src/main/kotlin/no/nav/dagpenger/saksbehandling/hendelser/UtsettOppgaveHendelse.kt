package no.nav.dagpenger.saksbehandling.hendelser

import java.time.LocalDate
import java.util.UUID

data class UtsettOppgaveHendelse(
    val oppgaveId: UUID,
    val navIdent: String,
    val utsattTil: LocalDate,
    val beholdOppgave: Boolean,
    override val utførtAv: String,
) : Hendelse(utførtAv)
