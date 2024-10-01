package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør
import java.util.UUID

class KlarTilKontrollHendelse(
    val oppgaveId: UUID,
    override val utførtAv: Aktør,
) : Hendelse(utførtAv)
