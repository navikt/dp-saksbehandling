package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.api.AvbrytBehandlingHendelse
import no.nav.dagpenger.saksbehandling.api.BekreftOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class MediatorTest {
    private val testIdent = "12345612345"
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()

    private val testRapid = TestRapid()
    private val personRepository = InMemoryPersonRepository()
    private val mediator = Mediator(personRepository = personRepository, behandlingKlient = mockk())

    init {
        BehandlingOpprettetMottak(testRapid, mediator)
    }

    @AfterEach
    fun tearDown() {
        personRepository.slettAlt()
    }

    @Test
    fun `Skal bare hente oppgaver med tilstand klar til behandling`() {
        personRepository.lagre(
            Person(
                ident = testIdent,

            ).apply {
                val klarBehandling = Behandling(
                    behandlingId = UUIDv7.ny(),
                    oppgave = Oppgave(
                        oppgaveId = UUIDv7.ny(),
                        behandlingId = behandlingId,
                        ident = this.ident,
                        emneknagger = setOf("Søknadsbehandling"),
                        opprettet = ZonedDateTime.now(),
                        tilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                    ),
                )
                val opprettetBehandling = Behandling(
                    behandlingId = UUIDv7.ny(),
                    oppgave = Oppgave(
                        oppgaveId = UUIDv7.ny(),
                        behandlingId = behandlingId,
                        ident = this.ident,
                        emneknagger = setOf("Søknadsbehandling"),
                        opprettet = ZonedDateTime.now(),
                        tilstand = OPPRETTET,
                    ),
                )
                this.behandlinger[klarBehandling.behandlingId] = klarBehandling
                this.behandlinger[opprettetBehandling.behandlingId] = opprettetBehandling
            },
        )
        val mediator = Mediator(personRepository, mockk())

        mediator.hentAlleOppgaverMedTilstand(Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING).size shouldBe 1
    }

    @Test
    fun `Tester endring av oppgavens tilstand etter hvert som behandling skjer`() {
        personRepository.slettAlt()

        val førsteSøknadId = UUIDv7.ny()
        val førsteBehandlingId = UUIDv7.ny()
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
        mediator.hentAlleOppgaver().size shouldBe 0

        // Behandling av første søknad
        mediator.behandle(
            ForslagTilVedtakHendelse(ident = testIdent, søknadId = førsteSøknadId, behandlingId = førsteBehandlingId),
        )

        mediator.hentAlleOppgaver().size shouldBe 1
        val oppgave = mediator.hentAlleOppgaver().first()
        oppgave.behandlingId shouldBe førsteBehandlingId

        runBlocking {
            mediator.bekreftOppgavensOpplysninger(BekreftOppgaveHendelse(oppgave.oppgaveId, saksbehandlerSignatur = ""))
        }

        mediator.hentAlleOppgaver().size shouldBe 0
        mediator.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET).size shouldBe 1

        // Behandling av andre søknad
        mediator.behandle(
            ForslagTilVedtakHendelse(ident = testIdent, søknadId = andreSøknadId, behandlingId = andreBehandlingId),
        )

        mediator.hentAlleOppgaver().size shouldBe 1
        val oppgave2 = mediator.hentAlleOppgaver().first()
        oppgave2.behandlingId shouldBe andreBehandlingId

        runBlocking {
            mediator.avbrytBehandling(AvbrytBehandlingHendelse(oppgave2.oppgaveId, saksbehandlerSignatur = ""))
        }

        mediator.hentAlleOppgaver().size shouldBe 0
        mediator.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET).size shouldBe 2
    }

    @Test
    fun `Lagre ny søknadsbehandling`() {
        testRapid.sendTestMessage(søknadsbehandlingOpprettetMelding(testIdent))
        val person = personRepository.hent(testIdent)
        requireNotNull(person)
        person.ident shouldBe testIdent
        person.behandlinger.size shouldBe 1
        person.behandlinger.get(behandlingId)?.oppgave shouldNotBe null
        val oppgaver = personRepository.hentAlleOppgaver()
        oppgaver.size shouldBe 2
        personRepository.hent(oppgaveId = oppgaver.first().oppgaveId) shouldNotBe null
    }

    @Language("JSON")
    private fun søknadsbehandlingOpprettetMelding(
        ident: String,
        søknadId: UUID = this.søknadId,
        behandlingId: UUID = this.behandlingId,
    ) =
        """
        {
            "@event_name": "behandling_opprettet",
            "@opprettet": "2024-01-30T10:43:32.988331190",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "søknadId": "$søknadId",
            "behandlingId": "$behandlingId",
            "ident": "$ident"
        }
        """
}
