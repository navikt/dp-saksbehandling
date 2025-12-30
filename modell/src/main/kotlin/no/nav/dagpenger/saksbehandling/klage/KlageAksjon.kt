package no.nav.dagpenger.saksbehandling.klage

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

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
        val fullmektigData: Map<OpplysningType, String> = emptyMap(),
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
                fullmektigData.forEach { (opplysningType, verdi) ->
                    when (opplysningType) {
                        OpplysningType.FULLMEKTIG_NAVN -> data["prosessfullmektigNavn"] = verdi
                        OpplysningType.FULLMEKTIG_ADRESSE_1 -> data["prosessfullmektigAdresselinje1"] = verdi
                        OpplysningType.FULLMEKTIG_ADRESSE_2 -> data["prosessfullmektigAdresselinje2"] = verdi
                        OpplysningType.FULLMEKTIG_ADRESSE_3 -> data["prosessfullmektigAdresselinje3"] = verdi
                        OpplysningType.FULLMEKTIG_POSTNR -> data["prosessfullmektigPostnummer"] = verdi
                        OpplysningType.FULLMEKTIG_POSTSTED -> data["prosessfullmektigPoststed"] = verdi
                        OpplysningType.FULLMEKTIG_LAND -> data["prosessfullmektigLand"] = verdi
                        else -> {
                            logger.warn { "OpplysningType $opplysningType støttes ikke når fullmektig data skal sendes til KA" }
                        }
                    }
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
