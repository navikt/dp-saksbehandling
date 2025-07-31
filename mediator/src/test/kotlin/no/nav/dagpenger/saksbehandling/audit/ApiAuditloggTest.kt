package no.nav.dagpenger.saksbehandling.audit

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AktivitetsloggMediator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApiAuditloggTest {
    private val testRapid = TestRapid()
    private val testIdent = "12345678901"
    private val testSaksbehandlerIdent = "12345678902"

    private val auditlogg =
        ApiAuditlogg(
            aktivitetsloggMediator = AktivitetsloggMediator(),
            rapidsConnection = testRapid,
        )

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun les() {
        auditlogg.les("Så en klagebehandling", testIdent, testSaksbehandlerIdent)
        testRapid.inspektør.size shouldBe 1
    }

    @Test
    fun opprett() {
        auditlogg.opprett("Opprettet en klagebehandling", testIdent, testSaksbehandlerIdent)
        testRapid.inspektør.size shouldBe 1
    }

    @Test
    fun oppdater() {
        auditlogg.opprett("Oppdaterte en klagebehandling", testIdent, testSaksbehandlerIdent)
        testRapid.inspektør.size shouldBe 1
    }

    @Test
    fun slett() {
        auditlogg.opprett("Slettet en klagebehandling", testIdent, testSaksbehandlerIdent)
        testRapid.inspektør.size shouldBe 1
    }

    private fun JsonNode.aktivitetsloggMelding(): String {
        require(this["@event_name"].asText() == "aktivitetslogg") { "Forventet behov som aktivitetslogg" }
        return "TODO: Jeg har lyst å bruke denne for å hente ut meldingen som sendes inn til auditloggen, men jeg vet ikke hvordan! "
    }
}
