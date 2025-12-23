package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.klage.KlageAksjon.IngenAksjon
import no.nav.dagpenger.saksbehandling.klage.KlageAksjon.JournalpostTilKA
import no.nav.dagpenger.saksbehandling.klage.KlageAksjon.OversendKlageinstans
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KlageAksjonTest {
    @Test
    fun `OversendKlageinstans behovData inneholder alle påkrevde felter`() {
        val behandlingId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val aksjon =
            OversendKlageinstans(
                behandlingId = behandlingId,
                ident = "12345678901",
                fagsakId = "SAK123",
                behandlendeEnhet = "4450",
                hjemler = listOf("§4-2", "§4-3"),
                opprettet = opprettet,
                tilknyttedeJournalposter =
                    listOf(
                        JournalpostTilKA(type = "VEDTAK", journalpostId = "JP001"),
                        JournalpostTilKA(type = "KLAGEMELDING", journalpostId = "JP002"),
                    ),
            )

        val behovData = aksjon.behovData()

        behovData["behandlingId"] shouldBe behandlingId.toString()
        behovData["ident"] shouldBe "12345678901"
        behovData["fagsakId"] shouldBe "SAK123"
        behovData["behandlendeEnhet"] shouldBe "4450"
        behovData["hjemler"] shouldBe listOf("§4-2", "§4-3")
        behovData["opprettet"] shouldBe opprettet
        behovData["tilknyttedeJournalposter"] shouldBe
            listOf(
                mapOf("type" to "VEDTAK", "journalpostId" to "JP001"),
                mapOf("type" to "KLAGEMELDING", "journalpostId" to "JP002"),
            )
    }

    @Test
    fun `OversendKlageinstans behovData inkluderer fullmektigData når present`() {
        val aksjon =
            OversendKlageinstans(
                behandlingId = UUID.randomUUID(),
                ident = "12345678901",
                fagsakId = "SAK123",
                behandlendeEnhet = "4450",
                hjemler = listOf("§4-2"),
                opprettet = LocalDateTime.now(),
                tilknyttedeJournalposter = emptyList(),
                fullmektigData =
                    mapOf(
                        "fullmektigNavn" to "Advokat Hansen",
                        "fullmektigAdresse1" to "Storgata 1",
                    ),
            )

        val behovData = aksjon.behovData()

        behovData shouldContainAll
            mapOf(
                "fullmektigNavn" to "Advokat Hansen",
                "fullmektigAdresse1" to "Storgata 1",
            )
    }

    @Test
    fun `OversendKlageinstans behovData inkluderer kommentar når present`() {
        val aksjon =
            OversendKlageinstans(
                behandlingId = UUID.randomUUID(),
                ident = "12345678901",
                fagsakId = "SAK123",
                behandlendeEnhet = "4450",
                hjemler = listOf("§4-2"),
                opprettet = LocalDateTime.now(),
                tilknyttedeJournalposter = emptyList(),
                kommentar = "Dette er en intern melding",
            )

        val behovData = aksjon.behovData()

        behovData["kommentar"] shouldBe "Dette er en intern melding"
    }

    @Test
    fun `OversendKlageinstans behovData ekskluderer kommentar når null`() {
        val aksjon =
            OversendKlageinstans(
                behandlingId = UUID.randomUUID(),
                ident = "12345678901",
                fagsakId = "SAK123",
                behandlendeEnhet = "4450",
                hjemler = listOf("§4-2"),
                opprettet = LocalDateTime.now(),
                tilknyttedeJournalposter = emptyList(),
                kommentar = null,
            )

        val behovData = aksjon.behovData()

        behovData.containsKey("kommentar") shouldBe false
    }

    @Test
    fun `IngenAksjon har korrekt behandlingId`() {
        val behandlingId = UUID.randomUUID()
        val aksjon = IngenAksjon(behandlingId = behandlingId)

        aksjon.behandlingId shouldBe behandlingId
    }

    @Test
    fun `JournalpostTilKA har korrekte verdier`() {
        val journalpost = JournalpostTilKA(type = "VEDTAK", journalpostId = "JP123")

        journalpost.type shouldBe "VEDTAK"
        journalpost.journalpostId shouldBe "JP123"
    }

    @Test
    fun `OversendKlageinstans med tom fullmektigData inkluderer ikke ekstra felter`() {
        val aksjon =
            OversendKlageinstans(
                behandlingId = UUID.randomUUID(),
                ident = "12345678901",
                fagsakId = "SAK123",
                behandlendeEnhet = "4450",
                hjemler = listOf("§4-2"),
                opprettet = LocalDateTime.now(),
                tilknyttedeJournalposter = emptyList(),
                fullmektigData = emptyMap(),
            )

        val behovData = aksjon.behovData()

        behovData.keys.none { it.startsWith("fullmektig") } shouldBe true
    }

    @Test
    fun `OversendKlageinstans med flere journalposter formaterer korrekt`() {
        val aksjon =
            OversendKlageinstans(
                behandlingId = UUID.randomUUID(),
                ident = "12345678901",
                fagsakId = "SAK123",
                behandlendeEnhet = "4450",
                hjemler = listOf("§4-2"),
                opprettet = LocalDateTime.now(),
                tilknyttedeJournalposter =
                    listOf(
                        JournalpostTilKA(type = "VEDTAK", journalpostId = "JP001"),
                        JournalpostTilKA(type = "KLAGEMELDING", journalpostId = "JP002"),
                        JournalpostTilKA(type = "ANNET", journalpostId = "JP003"),
                    ),
            )

        val behovData = aksjon.behovData()
        val journalposter = behovData["tilknyttedeJournalposter"] as List<*>

        journalposter.size shouldBe 3
    }
}
