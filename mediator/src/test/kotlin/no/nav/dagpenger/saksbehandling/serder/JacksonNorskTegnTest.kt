package no.nav.dagpenger.saksbehandling.serder

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.FjernOppgaveAnsvarÅrsak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Verifiserer at Jackson serialiserer Kotlin-properties som starter med norske tegn (ø, å, æ).
 *
 * Jackson 3 er strengere på getter-navngiving: en property `årsak` genererer getteren `getårsak()`
 * i Java, som Jackson 3 ikke plukker opp som standard fordi `å` ikke er ASCII uppercase etter `get`.
 * Disse feltene droppes LYDLØST fra JSON uten feil eller advarsel.
 *
 * Se: https://github.com/FasterXML/jackson-module-kotlin/issues/1154
 */
class JacksonNorskTegnTest {
    private val mapper = defaultObjectMapper()

    private val hendelse =
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

    @Test
    fun `årsak skal være med i serialisert JSON`() {
        val json = mapper.writeValueAsString(hendelse)
        val node = mapper.readTree(json)

        node.has("årsak") shouldBe true
        node["årsak"].asText() shouldBe "INHABILITET"
    }

    @Test
    fun `årsak skal deserialiseres korrekt fra JSON`() {
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
}
