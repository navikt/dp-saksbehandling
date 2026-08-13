package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.time.LocalDateTime
import java.util.UUID

data class KlageinstansVedtakHendelse(
    val type: KlageinstansVedtakType,
    val klageId: UUID,
    val klageinstansVedtakId: UUID,
    val avsluttet: LocalDateTime,
    val utfall: String,
    val journalpostIder: List<String>,
    override val utførtAv: Applikasjon = Applikasjon.Kabal,
) : Hendelse(utførtAv) {
    enum class KlageinstansVedtakType {
        KLAGE,
        ;

        companion object {
            fun fromString(type: String): KlageinstansVedtakType =
                when (type.uppercase()) {
                    "KLAGEBEHANDLING_AVSLUTTET" -> KLAGE
                    else -> throw IllegalArgumentException("Ukjent klageinstans vedtak type: $type")
                }
        }
    }
}
