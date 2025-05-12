package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import java.time.LocalDateTime

data class KlageMottattHendelse(
    val ident: String,
    val opprettet: LocalDateTime,
    val journalpostId: String?,
    override val utførtAv: Behandler = Applikasjon("dp-mottak"),
) : Hendelse(utførtAv)
