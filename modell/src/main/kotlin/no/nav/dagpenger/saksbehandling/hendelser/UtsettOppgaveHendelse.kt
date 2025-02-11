package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.Emneknagg.PåVent.ANNET
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.time.LocalDate
import java.util.UUID

data class UtsettOppgaveHendelse(
    val oppgaveId: UUID,
    val navIdent: String,
    val utsattTil: LocalDate,
    val beholdOppgave: Boolean,
    val årsak: Emneknagg.PåVent = ANNET,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
