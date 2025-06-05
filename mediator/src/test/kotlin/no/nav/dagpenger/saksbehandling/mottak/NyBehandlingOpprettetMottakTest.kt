package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class NyBehandlingOpprettetMottakTest {
    val testIdent = "12345678901"
    val søknadId = UUID.randomUUID()
    val meldekortId = 123L
    val behandlingId = UUID.randomUUID()
    val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.800935377")
    private val søknadsbehandlingOpprettetHendelse =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = behandlingId,
            ident = testIdent,
            opprettet = opprettet,
        )

    private val testRapid = TestRapid()
    private val sakMediatorMock = mockk<SakMediator>(relaxed = true)
    val pdlKlientMock =
        mockk<PDLKlient>().also {
            coEvery { it.erAdressebeskyttet(testIdent) }.returns(Result.success(false))
        }

    val skjermetKlientMock =
        mockk<SkjermingKlient>().also {
            coEvery { it.erSkjermetPerson(testIdent) }.returns(Result.success(false))
        }

    init {
        NyBehandlingOpprettetMottak(testRapid, sakMediatorMock, pdlKlientMock, skjermetKlientMock)
    }

    @Test
    fun `Skal behandle behandling_opprettet hendelse for søknadsbehandling`() {
        testRapid.sendTestMessage(søknadsbehandlingOpprettetMelding())
        verify(exactly = 1) {
            sakMediatorMock.opprettSak(
                søknadsbehandlingOpprettetHendelse = søknadsbehandlingOpprettetHendelse,
            )
        }
    }

    @Test
    fun `Skal behandle behandling_opprettet hendelse for meldekort`() {
        testRapid.sendTestMessage(meldekortbehandlingOpprettetMelding())
        verify(exactly = 1) {
            sakMediatorMock.knyttTilSak(
                meldekortbehandlingOpprettetHendelse =
                    MeldekortbehandlingOpprettetHendelse(
                        meldekortId = meldekortId,
                        behandlingId = behandlingId,
                        ident = testIdent,
                        opprettet = opprettet,
                    ),
            )
        }
    }

    @Language("JSON")
    private fun søknadsbehandlingOpprettetMelding(ident: String = testIdent) =
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
            "behandlingId": "$behandlingId",
            "ident": "$ident"
        }
        """

    @Language("JSON")
    private fun meldekortbehandlingOpprettetMelding(ident: String = testIdent) =
        """
        {
            "@event_name": "behandling_opprettet",
            "@opprettet": "$opprettet",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "behandletHendelse": {
                "datatype": "Long",
                "id": $meldekortId,
                "type": "Meldekort"
            },
            "behandlingId": "$behandlingId",
            "ident": "$ident"
        }
        """
}
