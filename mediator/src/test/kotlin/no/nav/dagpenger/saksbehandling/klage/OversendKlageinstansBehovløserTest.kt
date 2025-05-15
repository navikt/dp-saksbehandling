package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.lagPerson
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OversendKlageinstansBehovløserTest {
    private val ident = "12345612345"
    private val fagsakId = UUIDv7.ny()
    private val klageBehandling = lagKlagebehandling()
    private val oppgave =
        lagOppgave(
            behandling =
                lagBehandling(
                    person = lagPerson(ident = ident),
                    behandlingId = klageBehandling.behandlingId,
                    type = BehandlingType.KLAGE,
                ),
            opprettet = LocalDateTime.now(),
        )

    private val testRapid = TestRapid()
    private val klageRepositoryMock =
        mockk<KlageRepository>().also {
            coEvery {
                it.hentKlageBehandling(klageBehandling.behandlingId)
            } returns klageBehandling
        }
    private val klageKlient =
        mockk<KlageHttpKlient>().also {
            coEvery {
                it.registrerKlage(
                    klageBehandling = klageBehandling,
                    ident = ident,
                    fagsakId = any(),
                )
            } returns Result.success(HttpStatusCode.OK)
        }

    @Test
    fun `Skal løse behov dersom filter matcher`() {
        OversendKlageinstansBehovløser(
            rapidsConnection = testRapid,
            klageRepository = klageRepositoryMock,
            klageKlient = klageKlient,
        )
        testRapid.sendBehov()
    }

    private fun TestRapid.sendBehov() {
        this.sendTestMessage(
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "OversendelseKlageinstans" ],
              "behandlingId" : "$klageBehandling.behandlingId",
              "ident" : "$ident",
              "fagsakId" : "$fagsakId"
            }
            
            """.trimIndent(),
        )
    }
}
