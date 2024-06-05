package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingMediator
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class UtsendingMottakTest {
    private val testIdent = "12345678901"
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val testRapid = TestRapid()
    private val utsendingMediatorMock = mockk<UtsendingMediator>(relaxed = true)

    init {
        UtsendingMottak(testRapid, utsendingMediatorMock)
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse`() {
        val vedtakFattetHendelse = VedtakFattetHendelse(behandlingId = behandlingId, søknadId = søknadId, ident = testIdent)
        testRapid.sendTestMessage(vedtakFattetHendelse())
        verify(exactly = 1) {
            utsendingMediatorMock.mottaVedtakFattet(vedtakFattetHendelse)
        }
    }

    @Language("JSON")
    private fun vedtakFattetHendelse() =
        """
        {
            "@event_name": "vedtak_fattet",
            "søknadId": "$søknadId",
            "behandlingId": "$behandlingId",
            "ident": "$testIdent"
        }
        """
}
