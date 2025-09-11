package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class BehandlingOpprettetMottakTest {
    val testIdent = "12345678901"
    val søknadId = UUID.randomUUID()
    val meldekortId = "123"
    val manuellId = UUID.randomUUID()
    val behandlingIdNyRett = UUID.randomUUID()
    val behandlingIdGjenopptak = UUID.randomUUID()
    val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.800935377")
    private val søknadsbehandlingOpprettetHendelseNyRett =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = behandlingIdNyRett,
            ident = testIdent,
            opprettet = opprettet,
        )
    private val søknadsbehandlingOpprettetHendelseGjenopptak =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = behandlingIdGjenopptak,
            ident = testIdent,
            opprettet = opprettet,
            basertPåBehandling = behandlingIdNyRett,
        )

    private val testRapid = TestRapid()
    private val sakMediatorMock = mockk<SakMediator>(relaxed = true)

    init {
        BehandlingOpprettetMottak(testRapid, sakMediatorMock)
    }

    @Test
    fun `Skal behandle behandling_opprettet hendelse for søknadsbehandling av ny dagpengerett`() {
        testRapid.sendTestMessage(søknadsbehandlingOpprettetMeldingNyRett())
        verify(exactly = 1) {
            sakMediatorMock.opprettSak(
                søknadsbehandlingOpprettetHendelse = søknadsbehandlingOpprettetHendelseNyRett,
            )
        }
    }

    @Test
    fun `Skal behandle behandling_opprettet hendelse for søknadsbehandling som er basert på en annen behandling`() {
        testRapid.sendTestMessage(søknadsbehandlingOpprettetMeldingBasertPåBehandling())
        verify(exactly = 1) {
            sakMediatorMock.knyttTilSak(
                søknadsbehandlingOpprettetHendelse = søknadsbehandlingOpprettetHendelseGjenopptak,
            )
        }
    }

    @Test
    fun `Skal behandle behandling_opprettet hendelse for meldekort`() {
        val basertPåBehandling = UUIDv7.ny()
        testRapid.sendTestMessage(meldekortbehandlingOpprettetMelding(basertPåBehandling = basertPåBehandling))
        verify(exactly = 1) {
            sakMediatorMock.knyttTilSak(
                meldekortbehandlingOpprettetHendelse =
                    MeldekortbehandlingOpprettetHendelse(
                        meldekortId = meldekortId,
                        behandlingId = behandlingIdNyRett,
                        ident = testIdent,
                        opprettet = opprettet,
                        basertPåBehandling = basertPåBehandling,
                    ),
            )
        }
    }

    @Test
    fun `Skal behandle behandling_opprettet hendelse for manuell`() {
        val basertPåBehandling = UUIDv7.ny()
        testRapid.sendTestMessage(manuellbehandlingOpprettetMelding(basertPåBehandling = basertPåBehandling))
        verify(exactly = 1) {
            sakMediatorMock.knyttTilSak(
                manuellBehandlingOpprettetHendelse =
                    ManuellBehandlingOpprettetHendelse(
                        manuellId = manuellId,
                        behandlingId = behandlingIdNyRett,
                        ident = testIdent,
                        opprettet = opprettet,
                        basertPåBehandling = basertPåBehandling,
                    ),
            )
        }
    }

    @Test
    fun `fff`() {
        testRapid.sendTestMessage(tt)
        verify(exactly = 1) {
            sakMediatorMock.opprettSak(
                søknadsbehandlingOpprettetHendelse =
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = UUID.fromString("5343ef7e-b432-47a4-bb81-0942de7d81a6"),
                        behandlingId = UUID.fromString("01987947-7a09-7c11-a17d-c85f01326ead"),
                        ident = "14849598504",
                        opprettet = LocalDateTime.parse("2025-08-05T10:09:44.225832328"),
                    ),
            )
        }
    }

    @Language("JSON")
    private val tt =
        """
        {
          "@event_name": "behandling_opprettet",
          "ident": "14849598504",
          "behandlingId": "01987947-7a09-7c11-a17d-c85f01326ead",
          "basertPåBehandlinger": [
            "null"
          ],
          "behandletHendelse": {
            "id": "5343ef7e-b432-47a4-bb81-0942de7d81a6",
            "datatype": "UUID",
            "type": "Søknad"
          },
          "@id": "994db6d7-097c-476a-8c47-5823a518842e",
          "@opprettet": "2025-08-05T10:09:44.225832328",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "994db6d7-097c-476a-8c47-5823a518842e",
              "time": "2025-08-05T10:09:44.225832328",
              "service": "dp-behandling",
              "instance": "dp-behandling-8646d76c6d-2kph4",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-behandling:2025.08.05-08.07-303c86c"
            }
          ]
        }
        """.trimIndent()

    @Language("JSON")
    private fun søknadsbehandlingOpprettetMeldingNyRett(ident: String = testIdent) =
        """
        {
            "@event_name": "behandling_opprettet",
            "@opprettet": "$opprettet",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "behandletHendelse": {
                "datatype": "UUID",
                "id": "$søknadId",
                "type": "Søknad"
            },
            "behandlingId": "$behandlingIdNyRett",
            "ident": "$ident"
        }
        """

    @Language("JSON")
    private fun søknadsbehandlingOpprettetMeldingBasertPåBehandling(ident: String = testIdent) =
        """
        {
            "@event_name": "behandling_opprettet",
            "@opprettet": "$opprettet",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "behandletHendelse": {
                "datatype": "UUID",
                "id": "$søknadId",
                "type": "Søknad"
            },
            "basertPåBehandling": "$behandlingIdNyRett",
            "behandlingId": "$behandlingIdGjenopptak",
            "ident": "$ident"
        }
        """

    @Language("JSON")
    private fun meldekortbehandlingOpprettetMelding(
        ident: String = testIdent,
        basertPåBehandling: UUID,
    ) = """
        {
            "@event_name": "behandling_opprettet",
            "@opprettet": "$opprettet",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "behandletHendelse": {
                "datatype": "Long",
                "id": $meldekortId,
                "type": "Meldekort"
            },
            "basertPåBehandling": "$basertPåBehandling",
            "behandlingId": "$behandlingIdNyRett",
            "ident": "$ident"
        }
        """

    @Language("JSON")
    private fun manuellbehandlingOpprettetMelding(
        ident: String = testIdent,
        basertPåBehandling: UUID,
    ) = """
        {
            "@event_name": "behandling_opprettet",
            "@opprettet": "$opprettet",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "behandletHendelse": {
                "datatype": "UUID",
                "id": "$manuellId",
                "type": "Manuell"
            },
            "basertPåBehandling": "$basertPåBehandling",
            "behandlingId": "$behandlingIdNyRett",
            "ident": "$ident"
        }
        """
}
