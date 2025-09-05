package no.nav.dagpenger.saksbehandling.sak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import org.junit.jupiter.api.Test

class VedtakFattetMottakForSakTest {

    private val testRapid = TestRapid()
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val ident = "12345612345"

    @Test
    fun `skal oppdatere er_dp_dak hvis vedtak_fattet gjelder innvilgelse av søknad`() {

        val sakRepositoryMock = mockk<SakRepository>().also {
            every { it.settErDpSakForBehandling(any(), any()) } just Runs
        }
        VedtakFattetMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
        )

    }
    private fun vedtakFattetEvent(
        ident: String = this.ident,
        behandlingId: String = this.behandlingId.toString(),
        søknadId: String = this.søknadId.toString(),
        utfall: Boolean = true,
        behandletHendelseType: String = "Søknad",
    ): String {
        return """
            {
                "@event_name": "vedtak_fattet",
                "ident": "$ident",
                "behandlingId": "$behandlingId",
                "behandletHendelse": {
                    "id": "$søknadId",
                    "type": "$behandletHendelseType"
                },
                "fastsatt": {
                    "utfall": $utfall
                },
                "automatisk": false
            }
            """.trimIndent()
    }
}