package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.time.LocalDateTime
import java.util.UUID

data class KlageinstansVedtakHendelse(
    val type: KlageVedtakType,
    val klageId: UUID,
    val klageinstansVedtakId: UUID,
    val avsluttet: LocalDateTime,
    val utfall: String,
    val journalpostIder: List<String>,
    override val utførtAv: Applikasjon = Applikasjon("Kabal"),
) : Hendelse(utførtAv) {
    enum class KlageVedtakType {
        KLAGE,
        ;

        fun fromString(type: String): KlageVedtakType =
            when (type.uppercase()) {
                "KLAGEBEHANDLING_AVSLUTTET" -> KLAGE
                else -> throw IllegalArgumentException("Ukjent klage vedtak type: $type")
            }
    }
}
