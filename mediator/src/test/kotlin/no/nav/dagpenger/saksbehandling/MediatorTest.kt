package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.api.AvbrytBehandlingHendelse
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.maskinell.BehandlingKlient
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class MediatorTest {

    private val testIdent = "12345612345"
    private val testRapid = TestRapid()
    private val pdlKlientMock = mockk<PDLKlient>(relaxed = true)
    private val skjermingKlientMock = mockk<SkjermingKlient>(relaxed = true)
    private val behandlingKlient = mockk<BehandlingKlient>().also {
        coEvery { it.godkjennBehandling(any(), testIdent, any()) } returns 204
        coEvery { it.avbrytBehandling(any(), any(), any()) } returns 204
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir vedtatt`() {
        withMigratedDb { datasource ->
            val mediator = Mediator(
                repository = PostgresRepository(datasource),
                behandlingKlient = behandlingKlient,
                pdlKlient = pdlKlientMock,
            )

            BehandlingOpprettetMottak(testRapid, mediator, skjermingKlientMock, pdlKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()
            val søknadsbehandlingOpprettetHendelse = SøknadsbehandlingOpprettetHendelse(
                søknadId = søknadId,
                behandlingId = behandlingId,
                ident = testIdent,
                opprettet = ZonedDateTime.now(),
            )

            mediator.behandle(søknadsbehandlingOpprettetHendelse = søknadsbehandlingOpprettetHendelse)
            mediator.behandle(søknadsbehandlingOpprettetHendelse = søknadsbehandlingOpprettetHendelse)
            mediator.hentAlleOppgaverMedTilstand(OPPRETTET).size shouldBe 1

            mediator.behandle(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                ),
            )

            val oppgaverKlarTilBehandling = mediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
            oppgaverKlarTilBehandling.size shouldBe 1
            oppgaverKlarTilBehandling.single().behandlingId shouldBe behandlingId

            runBlocking {
                mediator.avsluttBehandling(
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        søknadId = søknadId,
                        ident = testIdent,
                    ),
                )
            }

            mediator.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET).size shouldBe 1
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir avbrutt`() {
        withMigratedDb { datasource ->
            val mediator = Mediator(
                repository = PostgresRepository(datasource),
                behandlingKlient = behandlingKlient,
                pdlKlient = pdlKlientMock,
            )

            BehandlingOpprettetMottak(testRapid, mediator, skjermingKlientMock, pdlKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()

            mediator.behandle(
                søknadsbehandlingOpprettetHendelse = SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = ZonedDateTime.now(),
                ),
            )
            mediator.hentAlleOppgaverMedTilstand(OPPRETTET).size shouldBe 1

            mediator.behandle(
                ForslagTilVedtakHendelse(ident = testIdent, søknadId = søknadId, behandlingId = behandlingId),
            )
            val oppgaverKlarTilBehandling = mediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
            oppgaverKlarTilBehandling.size shouldBe 1
            oppgaverKlarTilBehandling.single().behandlingId shouldBe behandlingId

            mediator.avbrytOppgave(
                BehandlingAvbruttHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = testIdent,
                ),
            )
            mediator.hentAlleOppgaverMedTilstand(AVBRUTT).size shouldBe 1
        }
    }

    @Test
    fun `Tester endring av oppgavens tilstand etter hvert som behandling skjer`() {
        withMigratedDb { datasource ->
            val mediator = Mediator(
                repository = PostgresRepository(datasource),
                behandlingKlient = behandlingKlient,
                pdlKlient = pdlKlientMock,
            )

            BehandlingOpprettetMottak(testRapid, mediator, skjermingKlientMock, pdlKlientMock)

            val førsteSøknadId = UUIDv7.ny()
            val førsteBehandlingId = UUIDv7.ny()
            val førsteSøknadsbehandlingOpprettetHendelse = SøknadsbehandlingOpprettetHendelse(
                søknadId = førsteSøknadId,
                behandlingId = førsteBehandlingId,
                ident = testIdent,
                opprettet = ZonedDateTime.now(),
            )

            mediator.behandle(søknadsbehandlingOpprettetHendelse = førsteSøknadsbehandlingOpprettetHendelse)
            mediator.behandle(søknadsbehandlingOpprettetHendelse = førsteSøknadsbehandlingOpprettetHendelse)
            mediator.hentAlleOppgaverMedTilstand(OPPRETTET).size shouldBe 1

            val andreSøknadId = UUIDv7.ny()
            val andreBehandlingId = UUIDv7.ny()
            mediator.behandle(
                søknadsbehandlingOpprettetHendelse = SøknadsbehandlingOpprettetHendelse(
                    søknadId = andreSøknadId,
                    behandlingId = andreBehandlingId,
                    ident = testIdent,
                    opprettet = ZonedDateTime.now(),
                ),
            )

            mediator.hentAlleOppgaverMedTilstand(OPPRETTET).size shouldBe 2
            mediator.hentOppgaverKlarTilBehandling().size shouldBe 0

            // Behandling av første søknad
            mediator.behandle(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = førsteSøknadId,
                    behandlingId = førsteBehandlingId,
                ),
            )

            mediator.hentOppgaverKlarTilBehandling().size shouldBe 1
            val oppgave = mediator.hentOppgaverKlarTilBehandling().single()
            oppgave.behandlingId shouldBe førsteBehandlingId

            runBlocking {
                mediator.avsluttBehandling(
                    VedtakFattetHendelse(
                        behandlingId = førsteBehandlingId,
                        søknadId = førsteSøknadId,
                        ident = testIdent,
                    ),
                )
            }

            mediator.hentOppgaverKlarTilBehandling().size shouldBe 0
            val ferdigBehandledeOppgaver = mediator.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET)
            ferdigBehandledeOppgaver.size shouldBe 1
            ferdigBehandledeOppgaver.single().behandlingId shouldBe førsteBehandlingId

            // Behandling av andre søknad
            mediator.behandle(
                ForslagTilVedtakHendelse(ident = testIdent, søknadId = andreSøknadId, behandlingId = andreBehandlingId),
            )

            mediator.hentOppgaverKlarTilBehandling().size shouldBe 1
            val oppgave2 = mediator.hentOppgaverKlarTilBehandling().first()
            oppgave2.behandlingId shouldBe andreBehandlingId

            runBlocking {
                mediator.avbrytBehandling(AvbrytBehandlingHendelse(oppgave2.oppgaveId, saksbehandlerSignatur = ""))
            }

            mediator.hentOppgaverKlarTilBehandling().size shouldBe 0
            mediator.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET).size shouldBe 2
        }
    }
}
