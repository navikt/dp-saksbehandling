package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import java.util.UUID

private val logger = KotlinLogging.logger { }

class KlageInstansBehovBygger(
    klageBehandling: KlageBehandling,
    sakId: UUID,
    hendelse: UtsendingDistribuert,
    finnJournalpostIdForBehandling: (UUID?) -> String?,
) {
    companion object {
        const val BEHOV_NAVN = "OversendelseKlageinstans"
    }

    val behov: String

    init {
        behov = byggBehov(klageBehandling, sakId, hendelse, finnJournalpostIdForBehandling)
    }

    private fun byggBehov(
        klageBehandling: KlageBehandling,
        sakId: UUID,
        hendelse: UtsendingDistribuert,
        finnJournalpostIdForBehandling: (UUID?) -> String?,
    ): String =
        JsonMessage.Companion
            .newNeed(
                behov = setOf(BEHOV_NAVN),
                mapOf<String, Any>(
                    "behandlingId" to klageBehandling.behandlingId.toString(),
                    "ident" to klageBehandling.personIdent(),
                    "fagsakId" to sakId.toString(),
                    "behandlendeEnhet" to (klageBehandling.behandlendeEnhet() ?: "4449"),
                    "hjemler" to klageBehandling.hjemler(),
                    "opprettet" to klageBehandling.opprettet,
                ) +
                    tilknyttedeJournalposter(
                        hendelse,
                        klageBehandling,
                        finnJournalpostIdForBehandling,
                    ) + kommentar(klageBehandling) + fullmektigData(klageBehandling),
            ).toJson()

    private fun fullmektigData(klageBehandling: KlageBehandling): Map<String, Any> {
        val fullmektigData =
            klageBehandling
                .synligeOpplysninger()
                .filter {
                    OpplysningBygger.fullmektigTilKlageinstansOpplysningTyper.contains(it.type) &&
                        it.verdi() != Verdi.TomVerdi
                }.associate { opplysning ->
                    opplysning.type to (opplysning.verdi() as Verdi.TekstVerdi).value
                }

        return buildMap {
            fullmektigData.forEach { (opplysningType, verdi) ->
                when (opplysningType) {
                    OpplysningType.FULLMEKTIG_NAVN -> this["prosessfullmektigNavn"] = verdi
                    OpplysningType.FULLMEKTIG_ADRESSE_1 -> this["prosessfullmektigAdresselinje1"] = verdi
                    OpplysningType.FULLMEKTIG_ADRESSE_2 -> this["prosessfullmektigAdresselinje2"] = verdi
                    OpplysningType.FULLMEKTIG_ADRESSE_3 -> this["prosessfullmektigAdresselinje3"] = verdi
                    OpplysningType.FULLMEKTIG_POSTNR -> this["prosessfullmektigPostnummer"] = verdi
                    OpplysningType.FULLMEKTIG_POSTSTED -> this["prosessfullmektigPoststed"] = verdi
                    OpplysningType.FULLMEKTIG_LAND -> this["prosessfullmektigLand"] = verdi
                    else -> {
                        logger.warn { "OpplysningType $opplysningType støttes ikke når fullmektig data skal sendes til KA" }
                    }
                }
            }
        }
    }

    private fun tilknyttedeJournalposter(
        hendelse: UtsendingDistribuert,
        klageBehandling: KlageBehandling,
        finnJournalpostIdForBehandling: (UUID?) -> String?,
    ): Map<String, List<Map<String, String>>> =
        mapOf(
            "tilknyttedeJournalposter" to
                buildList {
                    add(
                        mapOf(
                            "type" to "KLAGE_VEDTAK",
                            "journalpostId" to hendelse.journalpostId,
                        ),
                    )

                    klageBehandling.journalpostId()?.let { journalPostId ->
                        add(
                            mapOf(
                                "type" to "BRUKERS_KLAGE",
                                "journalpostId" to journalPostId,
                            ),
                        )
                    }
                    finnJournalpostIdForBehandling(klageBehandling.hentVedtakIdBrukerKlagerPå())?.let {
                        add(
                            mapOf(
                                "type" to "OPPRINNELIG_VEDTAK",
                                "journalpostId" to it,
                            ),
                        )
                    }
                },
        )

    private fun kommentar(klageBehandling: KlageBehandling): Map<String, String> =
        buildMap {
            klageBehandling.kommentarTilKlageInstans()?.let {
                this["kommentar"] = it
            }
        }
}
