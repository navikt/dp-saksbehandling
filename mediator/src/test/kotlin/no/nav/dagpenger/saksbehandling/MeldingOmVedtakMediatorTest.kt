package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient.KanIkkeLageMeldingOmVedtak
import kotlin.test.Test

class MeldingOmVedtakMediatorTest {
    private val oppgaveMediator = mockk<OppgaveMediator>()
    private val meldingOmVedtakKlient = mockk<MeldingOmVedtakKlient>()
    private val oppslag = mockk<Oppslag>()
    private val sakMediator = mockk<SakMediator>()

    private val mediator =
        MeldingOmVedtakMediator(
            oppgaveMediator = oppgaveMediator,
            meldingOmVedtakKlient = meldingOmVedtakKlient,
            oppslag = oppslag,
            sakMediator = sakMediator,
        )

    private val saksbehandler = TestHelper.saksbehandler
    private val saksbehandlerToken = "test-saksbehandler-token"
    private val oppgaveId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val sakId = UUIDv7.ny()
    private val oppgave =
        TestHelper.lagOppgave(
            oppgaveId = oppgaveId,
            behandling = TestHelper.lagBehandling(behandlingId = behandlingId),
            tilstandslogg =
                OppgaveTilstandslogg().also {
                    it.leggTil(
                        Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                        hendelse =
                            SettOppgaveAnsvarHendelse(
                                oppgaveId = oppgaveId,
                                ansvarligIdent = saksbehandler.navIdent,
                                utførtAv = saksbehandler,
                            ),
                    )
                },
        )

    private val behandlerDTO =
        BehandlerDTO(
            ident = saksbehandler.navIdent,
            fornavn = "Saks",
            etternavn = "Behandler",
            enhet =
                BehandlerDTOEnhetDTO(
                    navn = "Nav Enhet",
                    enhetNr = "1234",
                    postadresse = "Postadresse",
                ),
        )

    @Test
    fun `hentMeldingOmVedtakHtml - henter oppgave, person, saksbehandler, beslutter og sakId`() {
        every { oppgaveMediator.hentOppgave(oppgaveId, saksbehandler) } returns oppgave
        coEvery { oppslag.hentPerson(oppgave.personIdent()) } returns TestHelper.pdlPerson
        coEvery { oppslag.hentBehandler(saksbehandler.navIdent) } returns behandlerDTO
        every { sakMediator.hentSakIdForBehandlingId(behandlingId) } returns sakId
        coEvery {
            meldingOmVedtakKlient.hentMeldingOmVedtakHtml(
                person = TestHelper.pdlPerson,
                saksbehandler = behandlerDTO,
                beslutter = null,
                behandlingId = behandlingId,
                saksbehandlerToken = saksbehandlerToken,
                utløstAvType = UtløstAvType.SØKNAD,
                sakId = sakId.toString(),
            )
        } returns Result.success("<html>Vedtak</html>")

        runBlocking {
            val result =
                mediator.hentMeldingOmVedtakHtml(
                    oppgaveId = oppgaveId,
                    saksbehandler = saksbehandler,
                    saksbehandlerToken = saksbehandlerToken,
                )
            result shouldBe "<html>Vedtak</html>"
        }

        coVerify(exactly = 1) {
            oppslag.hentPerson(oppgave.personIdent())
            oppslag.hentBehandler(saksbehandler.navIdent)
            meldingOmVedtakKlient.hentMeldingOmVedtakHtml(
                person = TestHelper.pdlPerson,
                saksbehandler = behandlerDTO,
                beslutter = null,
                behandlingId = behandlingId,
                saksbehandlerToken = saksbehandlerToken,
                utløstAvType = UtløstAvType.SØKNAD,
                sakId = sakId.toString(),
            )
        }
    }

    @Test
    fun `hentMeldingOmVedtakHtml - sender med beslutter når oppgave har beslutter`() {
        val beslutterIdent = TestHelper.beslutter.navIdent
        val beslutterDTO =
            BehandlerDTO(
                ident = beslutterIdent,
                fornavn = "Be",
                etternavn = "Slutter",
                enhet =
                    BehandlerDTOEnhetDTO(
                        navn = "Nav Beslutter",
                        enhetNr = "5678",
                        postadresse = "Beslutter Postadresse",
                    ),
            )
        val oppgaveMedBeslutter =
            TestHelper.lagOppgave(
                oppgaveId = oppgaveId,
                behandling = TestHelper.lagBehandling(behandlingId = behandlingId),
                tilstandslogg = TestHelper.lagOppgaveTilstandslogg(),
            )

        every { oppgaveMediator.hentOppgave(oppgaveId, saksbehandler) } returns oppgaveMedBeslutter
        coEvery { oppslag.hentPerson(oppgaveMedBeslutter.personIdent()) } returns TestHelper.pdlPerson
        coEvery { oppslag.hentBehandler(saksbehandler.navIdent) } returns behandlerDTO
        coEvery { oppslag.hentBehandler(beslutterIdent) } returns beslutterDTO
        every { sakMediator.hentSakIdForBehandlingId(behandlingId) } returns sakId
        coEvery {
            meldingOmVedtakKlient.hentMeldingOmVedtakHtml(
                person = TestHelper.pdlPerson,
                saksbehandler = behandlerDTO,
                beslutter = beslutterDTO,
                behandlingId = behandlingId,
                saksbehandlerToken = saksbehandlerToken,
                utløstAvType = UtløstAvType.SØKNAD,
                sakId = sakId.toString(),
            )
        } returns Result.success("<html>Vedtak med beslutter</html>")

        runBlocking {
            val result =
                mediator.hentMeldingOmVedtakHtml(
                    oppgaveId = oppgaveId,
                    saksbehandler = saksbehandler,
                    saksbehandlerToken = saksbehandlerToken,
                )
            result shouldBe "<html>Vedtak med beslutter</html>"
        }

        coVerify(exactly = 1) {
            oppslag.hentBehandler(beslutterIdent)
        }
    }

    @Test
    fun `hentMeldingOmVedtakHtml - kaster feil når klient feiler`() {
        every { oppgaveMediator.hentOppgave(oppgaveId, saksbehandler) } returns oppgave
        coEvery { oppslag.hentPerson(oppgave.personIdent()) } returns TestHelper.pdlPerson
        coEvery { oppslag.hentBehandler(saksbehandler.navIdent) } returns behandlerDTO
        every { sakMediator.hentSakIdForBehandlingId(behandlingId) } returns sakId
        coEvery {
            meldingOmVedtakKlient.hentMeldingOmVedtakHtml(
                person = any(),
                saksbehandler = any(),
                beslutter = any(),
                behandlingId = any(),
                saksbehandlerToken = any(),
                utløstAvType = any(),
                sakId = any(),
            )
        } returns Result.failure(KanIkkeLageMeldingOmVedtak("Feil"))

        runBlocking {
            shouldThrow<KanIkkeLageMeldingOmVedtak> {
                mediator.hentMeldingOmVedtakHtml(
                    oppgaveId = oppgaveId,
                    saksbehandler = saksbehandler,
                    saksbehandlerToken = saksbehandlerToken,
                )
            }
        }
    }

    @Test
    fun `lagreUtvidetBeskrivelse - henter oppgave og delegerer til klient`() {
        val brevblokkId = "brevblokk-abc"
        val tekst = "En utvidet beskrivelse"
        every { oppgaveMediator.hentOppgave(oppgaveId, saksbehandler) } returns oppgave
        coEvery {
            meldingOmVedtakKlient.lagreUtvidetBeskrivelse(
                behandlingId = behandlingId,
                brevblokkId = brevblokkId,
                tekst = tekst,
                saksbehandlerToken = saksbehandlerToken,
            )
        } returns """{"sistEndretTidspunkt": "2025-01-01T12:00:00"}"""

        runBlocking {
            val result =
                mediator.lagreUtvidetBeskrivelse(
                    oppgaveId = oppgaveId,
                    brevblokkId = brevblokkId,
                    tekst = tekst,
                    saksbehandler = saksbehandler,
                    saksbehandlerToken = saksbehandlerToken,
                )
            result shouldBe """{"sistEndretTidspunkt": "2025-01-01T12:00:00"}"""
        }

        coVerify(exactly = 1) {
            meldingOmVedtakKlient.lagreUtvidetBeskrivelse(
                behandlingId = behandlingId,
                brevblokkId = brevblokkId,
                tekst = tekst,
                saksbehandlerToken = saksbehandlerToken,
            )
        }
    }

    @Test
    fun `lagreUtvidetBeskrivelse - kaster feil når klient feiler`() {
        val brevblokkId = "brevblokk-abc"
        every { oppgaveMediator.hentOppgave(oppgaveId, saksbehandler) } returns oppgave
        coEvery {
            meldingOmVedtakKlient.lagreUtvidetBeskrivelse(
                behandlingId = behandlingId,
                brevblokkId = brevblokkId,
                tekst = any(),
                saksbehandlerToken = any(),
            )
        } throws KanIkkeLageMeldingOmVedtak("Feil ved lagring")

        runBlocking {
            shouldThrow<KanIkkeLageMeldingOmVedtak> {
                mediator.lagreUtvidetBeskrivelse(
                    oppgaveId = oppgaveId,
                    brevblokkId = brevblokkId,
                    tekst = "tekst",
                    saksbehandler = saksbehandler,
                    saksbehandlerToken = saksbehandlerToken,
                )
            }
        }
    }

    @Test
    fun `lagreBrevVariant - henter oppgave og delegerer til klient`() {
        every { oppgaveMediator.hentOppgave(oppgaveId, saksbehandler) } returns oppgave
        coEvery {
            meldingOmVedtakKlient.lagreBrevVariant(
                behandlingId = behandlingId,
                brevVariant = "GENERERT",
                saksbehandlerToken = saksbehandlerToken,
            )
        } returns Unit

        runBlocking {
            mediator.lagreBrevVariant(
                oppgaveId = oppgaveId,
                brevVariant = "GENERERT",
                saksbehandler = saksbehandler,
                saksbehandlerToken = saksbehandlerToken,
            )
        }

        coVerify(exactly = 1) {
            meldingOmVedtakKlient.lagreBrevVariant(
                behandlingId = behandlingId,
                brevVariant = "GENERERT",
                saksbehandlerToken = saksbehandlerToken,
            )
        }
    }

    @Test
    fun `lagreBrevVariant - kaster feil når klient feiler`() {
        every { oppgaveMediator.hentOppgave(oppgaveId, saksbehandler) } returns oppgave
        coEvery {
            meldingOmVedtakKlient.lagreBrevVariant(
                behandlingId = behandlingId,
                brevVariant = any(),
                saksbehandlerToken = any(),
            )
        } throws KanIkkeLageMeldingOmVedtak("Feil ved lagring av brevvariant")

        runBlocking {
            shouldThrow<KanIkkeLageMeldingOmVedtak> {
                mediator.lagreBrevVariant(
                    oppgaveId = oppgaveId,
                    brevVariant = "EGENDEFINERT",
                    saksbehandler = saksbehandler,
                    saksbehandlerToken = saksbehandlerToken,
                )
            }
        }
    }
}
