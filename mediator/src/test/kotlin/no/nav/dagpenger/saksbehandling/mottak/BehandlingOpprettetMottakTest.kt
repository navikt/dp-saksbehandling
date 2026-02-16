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
import java.time.LocalDate
import java.util.UUID

class BehandlingOpprettetMottakTest {
    val testIdent = "12345678901"
    val søknadId = UUID.randomUUID()
    val meldekortId = "123"
    val manuellId = UUID.randomUUID()
    val behandlingIdNyRett = UUID.randomUUID()
    val behandlingIdGjenopptak = UUID.randomUUID()
    val behandletHendelseSkjedde = LocalDate.parse("2024-02-27")
    val behandlingskjedeId = UUIDv7.ny()
    private val søknadsbehandlingOpprettetHendelseNyRett =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = behandlingIdNyRett,
            ident = testIdent,
            opprettet = behandletHendelseSkjedde.atStartOfDay(),
            behandlingskjedeId = behandlingskjedeId,
        )
    private val søknadsbehandlingOpprettetHendelseGjenopptak =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = behandlingIdGjenopptak,
            ident = testIdent,
            opprettet = behandletHendelseSkjedde.atStartOfDay(),
            basertPåBehandling = behandlingIdNyRett,
            behandlingskjedeId = behandlingskjedeId,
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
        testRapid.sendTestMessage(
            meldekortbehandlingOpprettetMelding(
                basertPåBehandling = basertPåBehandling,
                behandlingskjedeId = behandlingskjedeId,
            ),
        )
        verify(exactly = 1) {
            sakMediatorMock.knyttTilSak(
                meldekortbehandlingOpprettetHendelse =
                    MeldekortbehandlingOpprettetHendelse(
                        meldekortId = meldekortId,
                        behandlingId = behandlingIdNyRett,
                        ident = testIdent,
                        opprettet = behandletHendelseSkjedde.atStartOfDay(),
                        basertPåBehandling = basertPåBehandling,
                        behandlingskjedeId = behandlingskjedeId,
                    ),
            )
        }
    }

    @Test
    fun `Skal behandle behandling_opprettet hendelse for manuell`() {
        val basertPåBehandling = UUIDv7.ny()
        testRapid.sendTestMessage(
            manuellBehandlingOpprettetMelding(
                basertPåBehandling = basertPåBehandling,
                behandlingskjedeId = behandlingskjedeId,
            ),
        )
        verify(exactly = 1) {
            sakMediatorMock.knyttTilSak(
                manuellBehandlingOpprettetHendelse =
                    ManuellBehandlingOpprettetHendelse(
                        manuellId = manuellId,
                        behandlingId = behandlingIdNyRett,
                        ident = testIdent,
                        opprettet = behandletHendelseSkjedde.atStartOfDay(),
                        basertPåBehandling = basertPåBehandling,
                        behandlingskjedeId = behandlingskjedeId,
                    ),
            )
        }
    }

    @Language("JSON")
    private fun søknadsbehandlingOpprettetMeldingNyRett(ident: String = testIdent) =
        """
        {
            "@event_name": "behandling_opprettet",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "behandlingskjedeId" : "$behandlingskjedeId",
            "behandletHendelse": {
                "datatype": "UUID",
                "id": "$søknadId",
                "type": "Søknad",
                "skjedde": "$behandletHendelseSkjedde"
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
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "behandlingskjedeId" : "$behandlingskjedeId",
            "behandletHendelse": {
                "datatype": "UUID",
                "id": "$søknadId",
                "type": "Søknad",
                "skjedde": "$behandletHendelseSkjedde"
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
        behandlingskjedeId: UUID,
    ) = """
        {
            "@event_name": "behandling_opprettet",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "behandlingskjedeId" : "$behandlingskjedeId",
            "behandletHendelse": {
                "datatype": "Long",
                "id": $meldekortId,
                "type": "Meldekort",
                "skjedde": "$behandletHendelseSkjedde"
            },
            "basertPåBehandling": "$basertPåBehandling",
            "behandlingId": "$behandlingIdNyRett",
            "ident": "$ident"
        }
        """

    @Language("JSON")
    private fun manuellBehandlingOpprettetMelding(
        ident: String = testIdent,
        basertPåBehandling: UUID,
        behandlingskjedeId: UUID,
    ) = """
        {
            "@event_name": "behandling_opprettet",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "behandlingskjedeId" : "$behandlingskjedeId",
            "behandletHendelse": {
                "datatype": "UUID",
                "id": "$manuellId",
                "type": "Manuell",
                "skjedde": "$behandletHendelseSkjedde"
            },
            "basertPåBehandling": "$basertPåBehandling",
            "behandlingId": "$behandlingIdNyRett",
            "ident": "$ident"
        }
        """
}
