package no.nav.dagpenger.saksbehandling.klage

import java.time.LocalDateTime
import java.util.UUID

sealed class KAVedtak {
    abstract val id: UUID
    abstract val journalpostIder: List<String>
    abstract val avsluttet: LocalDateTime

    abstract fun utfall(): String

    data class Klage(
        override val id: UUID,
        override val journalpostIder: List<String>,
        override val avsluttet: LocalDateTime,
        val utfall: Utfall,
    ) : KAVedtak() {
        override fun utfall(): String = utfall.name

        enum class Utfall {
            STADFESTELSE,
        }
    }
}
