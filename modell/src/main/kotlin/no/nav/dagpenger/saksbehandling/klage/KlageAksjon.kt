package no.nav.dagpenger.saksbehandling.klage

import java.time.LocalDateTime
import java.util.UUID

sealed class KlageAksjon {
    abstract val behandlingId: UUID

    data class IngenAksjon(
        override val behandlingId: UUID,
    ) : KlageAksjon()

    data class OversendKlageinstans(
        override val behandlingId: UUID,
        val ident: String,
        val fagsakId: String,
        val behandlendeEnhet: String,
        val hjemler: List<String>,
        val opprettet: LocalDateTime,
        val tilknyttedeJournalposter: List<JournalpostTilKA>,
        val fullmektigData: Map<String, String> = emptyMap(),
        val kommentar: String? = null,
    ) : KlageAksjon() {
        companion object {
            const val BEHOV_NAVN = "OversendelseKlageinstans"
        }

        fun behovData(): Map<String, Any> =
            mutableMapOf<String, Any>(
                "behandlingId" to behandlingId.toString(),
                "ident" to ident,
                "fagsakId" to fagsakId,
                "behandlendeEnhet" to behandlendeEnhet,
                "hjemler" to hjemler,
                "opprettet" to opprettet,
                "tilknyttedeJournalposter" to
                    tilknyttedeJournalposter.map {
                        mapOf(
                            "type" to it.type,
                            "journalpostId" to it.journalpostId,
                        )
                    },
            ).also { data ->
                fullmektigData.forEach { (key, value) ->
                    data[key] = value
                }
                kommentar?.let {
                    data["kommentar"] = it
                }
            }
    }

    data class JournalpostTilKA(
        val type: String,
        val journalpostId: String,
    )
}
