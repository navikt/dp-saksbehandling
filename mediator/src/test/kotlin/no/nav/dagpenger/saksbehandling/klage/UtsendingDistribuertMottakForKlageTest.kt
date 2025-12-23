package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.UUID

class UtsendingDistribuertMottakForKlageTest {
    private val testRapid = TestRapid()

    @Test
    fun `Skal håndtere  distribuert utsendining`() {
        val forVentetHendelse =
            UtsendingDistribuert(
                behandlingId = UUID.randomUUID(),
                distribusjonId = "distribusjon-123",
                journalpostId = "journalpost-456",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        val mockKlageMediator =
            mockk<KlageMediator>(relaxed = false).also {
                every { it.vedtakDistribuert(forVentetHendelse) } just Runs
            }

        UtsendingDistribuertMottakForKlage(
            rapidsConnection = testRapid,
            klageMediator = mockKlageMediator,
        )

        @Language("JSON")
        val melding =
            """
            {
              "@event_name": "utsending_distribuert",
              "behandlingId": "${forVentetHendelse.behandlingId}",
              "utsendingId": "${forVentetHendelse.utsendingId}",
              "distribusjonId": "${forVentetHendelse.distribusjonId}",
              "journalpostId": "${forVentetHendelse.journalpostId}",
              "ident": "${forVentetHendelse.ident}",
              "type": "KLAGEMELDING"
            }
            """.trimIndent()
        testRapid.sendTestMessage(melding)

        verify(exactly = 1) { mockKlageMediator.vedtakDistribuert(forVentetHendelse) }
    }
}
