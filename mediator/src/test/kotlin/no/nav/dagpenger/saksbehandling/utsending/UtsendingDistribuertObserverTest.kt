package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class UtsendingDistribuertObserverTest {
    private val rapid = TestRapid()

    @Test
    fun `skal publisere melding når utsending er distribuert`() {
        val behandlingId = UUID.randomUUID()
        val utsending = TestHelper.lagUtsending(tilstand = Utsending.Distribuert, behandlingId = behandlingId)
        val distribusjonId = "distribusjon-123"
        val journalpostId = "journalpost-456"

        UtsendingDistribuertObserver(rapidsConnection = rapid).onDistribuert(
            hendelse =
                DistribuertHendelse(
                    behandlingId = behandlingId,
                    distribusjonId = distribusjonId,
                    journalpostId = journalpostId,
                ),
            utsending = utsending,
        )

        rapid.inspektør.size shouldBe 1
        rapid.inspektør.message(0).toString().shouldEqualSpecifiedJson(
            """
            {
              "@event_name": "utsending_distribuert",
              "behandlingId": "$behandlingId",
              "utsendingId": "${utsending.id}",
              "distribusjonId": "$distribusjonId",
              "journalpostId": "$journalpostId",
              "ident": "${utsending.ident}",
              "type": "${utsending.type.name}"
            }
            """.trimIndent(),
        )
    }
}
