package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.db.DataNotFoundException
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class MediatorTest {
    private val testIdent = "12345612345"
    private val testRapid = TestRapid()
    private val pdlKlientMock = mockk<PDLKlient>(relaxed = true)
    private val skjermingKlientMock = mockk<SkjermingKlient>(relaxed = true)

    @Test
    fun `Livssyklus for søknadsbehandling som blir vedtatt`() {
        withMigratedDb { datasource ->
            val mediator =
                Mediator(
                    repository = PostgresRepository(datasource),
                )

            BehandlingOpprettetMottak(testRapid, mediator, skjermingKlientMock, pdlKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()
            val søknadsbehandlingOpprettetHendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = ZonedDateTime.now(),
                )

            mediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse = søknadsbehandlingOpprettetHendelse)
            mediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse = søknadsbehandlingOpprettetHendelse)
            mediator.hentAlleOppgaverMedTilstand(OPPRETTET).size shouldBe 1

            mediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                ),
            )

            val oppgaverKlarTilBehandling = mediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)

            oppgaverKlarTilBehandling.size shouldBe 1
            val oppgave = oppgaverKlarTilBehandling.single()
            oppgave.behandlingId shouldBe behandlingId

            mediator.tildelOppgave(
                OppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    navIdent = "NAVIdent",
                ),
            )

            val tildeltOppgave = mediator.hentOppgave(oppgave.oppgaveId)
            tildeltOppgave.tilstand() shouldBe UNDER_BEHANDLING
            tildeltOppgave.saksbehandlerIdent shouldBe "NAVIdent"

            mediator.avsluttBehandling(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = testIdent,
                ),
            )

            mediator.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET).size shouldBe 1
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir avbrutt`() {
        withMigratedDb { datasource ->
            val mediator =
                Mediator(
                    repository = PostgresRepository(datasource),
                )

            BehandlingOpprettetMottak(testRapid, mediator, skjermingKlientMock, pdlKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()

            mediator.opprettOppgaveForBehandling(
                søknadsbehandlingOpprettetHendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = ZonedDateTime.now(),
                ),
            )
            mediator.hentAlleOppgaverMedTilstand(OPPRETTET).size shouldBe 1

            mediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(ident = testIdent, søknadId = søknadId, behandlingId = behandlingId),
            )
            val oppgaver = mediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
            oppgaver.size shouldBe 1
            oppgaver.single().behandlingId shouldBe behandlingId

            mediator.avbrytOppgave(
                BehandlingAvbruttHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = testIdent,
                ),
            )

            assertThrows<DataNotFoundException> {
                mediator.hentOppgave(oppgaver.single().oppgaveId)
            }
        }
    }
}
