package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.api.AvbrytBehandlingHendelse
import no.nav.dagpenger.saksbehandling.api.BekreftOppgaveHendelse
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresRepository
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class MediatorTest {

    @Test
    fun `Tester endring av oppgavens tilstand etter hvert som behandling skjer`() {
        withMigratedDb { datasource ->
            val testIdent = "12345612345"
            val testRapid = TestRapid()
            val postgresRepository = PostgresRepository(datasource)
            val mediator = Mediator(repository = postgresRepository, behandlingKlient = mockk())
            val førsteSøknadId = UUIDv7.ny()
            val førsteBehandlingId = UUIDv7.ny()
            BehandlingOpprettetMottak(testRapid, mediator)

            mediator.behandle(
                søknadsbehandlingOpprettetHendelse = SøknadsbehandlingOpprettetHendelse(
                    søknadId = førsteSøknadId,
                    behandlingId = førsteBehandlingId,
                    ident = testIdent,
                    opprettet = ZonedDateTime.now(),
                ),
            )

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
                ForslagTilVedtakHendelse(ident = testIdent, søknadId = førsteSøknadId, behandlingId = førsteBehandlingId),
            )

            mediator.hentOppgaverKlarTilBehandling().size shouldBe 1
            val oppgave = mediator.hentOppgaverKlarTilBehandling().single()
            oppgave.behandlingId shouldBe førsteBehandlingId

            runBlocking {
                mediator.bekreftOppgavensOpplysninger(BekreftOppgaveHendelse(oppgave.oppgaveId, saksbehandlerSignatur = ""))
            }

            mediator.hentOppgaverKlarTilBehandling().size shouldBe 0
            val hentAlleOppgaverMedTilstand = mediator.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET)
            hentAlleOppgaverMedTilstand.size shouldBe 1
            hentAlleOppgaverMedTilstand.single().behandlingId shouldBe førsteBehandlingId

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
