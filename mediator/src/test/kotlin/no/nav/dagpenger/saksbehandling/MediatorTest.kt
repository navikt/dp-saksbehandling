package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class MediatorTest {
    private val testIdent = "12345612345"
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()

    private val testRapid = TestRapid()
    private val personRepository = InMemoryPersonRepository()
    private val mediator = Mediator(personRepository, mockk())

    init {
        BehandlingOpprettetMottak(testRapid, mediator)
    }

    @AfterEach
    fun tearDown() {
        personRepository.slettAlt()
    }

    @Test
    fun `Skal bare hente oppgaver med tilstand KLAR_FOR_BEHANDLING`() {
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
                        tilstand = Oppgave.Tilstand.Type.OPPRETTET,
                    ),
                )
                this.behandlinger[klarBehandling.behandlingId] = klarBehandling
                this.behandlinger[opprettetBehandling.behandlingId] = opprettetBehandling
            },
        )
        val mediator = Mediator(personRepository, mockk())

        mediator.hentAlleOppgaveMedTilstand(Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING).size shouldBe 1
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
    private fun søknadsbehandlingOpprettetMelding(ident: String) =
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
