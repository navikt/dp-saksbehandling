package no.nav.dagpenger.saksbehandling.serder

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.dagpenger.saksbehandling.FjernOppgaveAnsvarÅrsak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

/**
 * Tester kontrakten til [defaultObjectMapper] — den sentraliserte Jackson-konfigurasjonen
 * for Dagpenger-applikasjoner.
 *
 * Dekker:
 * - Norske tegn i property-navn (withFirstCharAcceptance)
 * - Null-ekskludering (NON_NULL)
 * - Dato-serialisering som ISO-strenger (ikke timestamps)
 */
class DefaultObjectMapperTest {
    private val mapper = defaultObjectMapper()

    @Test
    fun `properties med norske tegn skal serialiseres korrekt`() {
        val hendelse =
            FjernOppgaveAnsvarHendelse(
                oppgaveId = UUID.fromString("018fa4fe-8450-7fa2-9e47-cafa81f718cd"),
                årsak = FjernOppgaveAnsvarÅrsak.INHABILITET,
                utførtAv =
                    Saksbehandler(
                        navIdent = "Z999999",
                        grupper = emptySet(),
                        tilganger = setOf(TilgangType.SAKSBEHANDLER),
                    ),
            )

        val json = mapper.writeValueAsString(hendelse)
        val node = mapper.readTree(json)

        node.has("årsak") shouldBe true
        node["årsak"].asText() shouldBe "INHABILITET"
    }

    @Test
    fun `properties med norske tegn skal deserialiseres korrekt`() {
        val json =
            """
            {
                "oppgaveId": "018fa4fe-8450-7fa2-9e47-cafa81f718cd",
                "årsak": "INHABILITET",
                "utførtAv": {
                    "navIdent": "Z999999",
                    "grupper": [],
                    "tilganger": ["SAKSBEHANDLER"]
                }
            }
            """.trimIndent()

        val deserialisert = mapper.readValue(json, FjernOppgaveAnsvarHendelse::class.java)
        deserialisert.årsak shouldBe FjernOppgaveAnsvarÅrsak.INHABILITET
    }

    @Test
    fun `intern objectMapper ekskluderer null-felter`() {
        data class MedNullbart(
            val navn: String,
            val beskrivelse: String? = null,
        )

        val json = objectMapper.writeValueAsString(MedNullbart(navn = "test"))

        json shouldContain "\"navn\""
        json shouldNotContain "beskrivelse"
    }

    @Test
    fun `defaultObjectMapper inkluderer null-felter eksplisitt`() {
        data class MedNullbart(
            val navn: String,
            val beskrivelse: String? = null,
        )

        val json = mapper.writeValueAsString(MedNullbart(navn = "test"))

        json shouldContain "\"navn\""
        json shouldContain "beskrivelse"
    }

    @Test
    fun `datoer skal serialiseres som ISO-strenger, ikke timestamps`() {
        data class MedDato(
            val opprettet: LocalDateTime,
        )

        val dato = LocalDateTime.of(2026, 4, 30, 14, 30, 0)
        val json = mapper.writeValueAsString(MedDato(opprettet = dato))

        json shouldContain "2026-04-30T14:30:00"
        json shouldNotContain "1746" // Ikke epoch-millis
    }
}
