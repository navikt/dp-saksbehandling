package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Oppgave.KontrollertBrev
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

data class LagreBrevKvitteringHendelse(
    val oppgaveId: UUID,
    val kontrollertBrev: KontrollertBrev,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
