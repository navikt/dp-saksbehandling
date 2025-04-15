package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import java.time.LocalDateTime
import java.util.UUID

data class KlageMottattHendelse(
    val klageId: UUID = UUIDv7.ny(),
    val ident: String,
    val opprettet: LocalDateTime,
    val journalpostId: String,
    override val utførtAv: Behandler = Applikasjon("dp-mottak"),
) : Hendelse(utførtAv)
