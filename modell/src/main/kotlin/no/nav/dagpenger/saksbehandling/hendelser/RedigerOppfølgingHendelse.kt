package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.time.LocalDate
import java.util.UUID

data class RedigerOppfølgingHendelse(
    val oppfølgingId: UUID,
    val tittel: String,
    val beskrivelse: String,
    val frist: LocalDate?,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
