package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testIdent
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSporing
import no.nav.dagpenger.behandling.Tilstand.Utført
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import no.nav.dagpenger.behandling.hendelser.StegUtført
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.oppgave.InMemoryOppgaveRepository
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.prosess.Arbeidsprosess
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

class MediatorTest() {
    private val testRapid = TestRapid()
    private val ident = testIdent
    private var oppgave: Oppgave
    private var oppgaveId: UUID

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `Behandle BehandlingSvar hendelse`() {
        mediator.behandle(StegUtført(ident, oppgaveId)) {
            besvar(finnStegId("vilkår1"), false, testSporing)
        }

        mediator.behandle(StegUtført(ident, oppgaveId)) {
            besvar(finnStegId("vilkår 1 dato"), LocalDate.now(), testSporing)
        }

        mediator.behandle(StegUtført(ident, oppgaveId)) {
            besvar(finnStegId("fastsettelse1"), 2, testSporing)
        }
    }

    @Test
    fun `Behandle SøknadBehandlet`() {
        mediator.behandle(SøknadInnsendtHendelse(UUID.randomUUID(), "123", testIdent))
        val oppgaveId = mockOppgaveRepository.hentOppgaver().last().uuid
        val oppgave = mockOppgaveRepository.hentOppgave(oppgaveId)
        mediator.behandle(StegUtført(testIdent, oppgaveId)) {
            oppgave.alleSteg().forEach {
                when (it.svar.clazz.simpleName) {
                    "Boolean" -> besvar(it.uuid, true, testSporing)
                    "Integer" -> besvar(it.uuid, Random.nextInt(), testSporing)
                    "String" -> besvar(it.uuid, Random.nextBytes(10).toString(), testSporing)
                    "LocalDate" -> besvar(it.uuid, LocalDate.now(), testSporing)
                    "Double" -> besvar(it.uuid, Random.nextDouble(), testSporing)
                }
            }

            oppgave.alleSteg().forEach { it.tilstand shouldBe Utført }
        }
        oppgave.gåTil("Innstilt")
        oppgave.gåTil("Vedtak")

        testRapid.inspektør.size shouldBe 2
        val event = testRapid.inspektør.message(0)
        val partitionKey = testRapid.inspektør.key(0)

        partitionKey shouldBe ident
        event["@event_name"].asText() shouldBe "søknad_behandlet_hendelse"
    }

    private val mockOppgaveRepository = InMemoryOppgaveRepository().apply {
        oppgave = Oppgave(
            behandling(testPerson, testHendelse) {
                steg {
                    vilkår("vilkår1") {
                        avhengerAvFastsettelse<LocalDate>("vilkår 1 dato")
                    }
                }
                steg {
                    fastsettelse<Int>("fastsettelse1")
                }
            },
            Arbeidsprosess().apply {
                leggTilTilstand("Start")
                start("Start")
            },
        )
        oppgaveId = oppgave.uuid
        lagreOppgave(oppgave)
        lagreOppgave(SøknadInnsendtHendelse(UUID.randomUUID(), "", "20987654321").oppgave())
    }
    private val mediator = Mediator(
        rapidsConnection = testRapid,
        oppgaveRepository = mockOppgaveRepository,
    )

    private fun finnStegId(id: String) = oppgave.alleSteg().single { it.id == id }.uuid
}
