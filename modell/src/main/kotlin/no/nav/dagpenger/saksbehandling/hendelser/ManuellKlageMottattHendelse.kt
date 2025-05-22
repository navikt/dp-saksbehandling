package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.time.LocalDateTime

data class ManuellKlageMottattHendelse(
    val ident: String,
    val opprettet: LocalDateTime,
    val journalpostId: String?,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
