package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.time.LocalDateTime

data class KlageMottattHendelse(
    val ident: String,
    val opprettet: LocalDateTime,
    val journalpostId: String?,
    override val utførtAv: Applikasjon = Applikasjon("dp-mottak"),
) : Hendelse(utførtAv)
