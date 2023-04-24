package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Meldingsfabrikk.testIdent
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import no.nav.dagpenger.behandling.hendelser.StegUtført
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.oppgave.InMemoryOppgaveRepository
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.prosess.Arbeidsprosess
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class MediatorTest {
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
            besvar(finnStegId("vilkår1"), false)
        }

        mediator.behandle(StegUtført(ident, oppgaveId)) {
            besvar(finnStegId("vilkår 1 dato"), LocalDate.now())
        }

        mediator.behandle(StegUtført(ident, oppgaveId)) {
            besvar(finnStegId("fastsettelse1"), 2)
        }
    }

    @Test
    @Disabled
    fun `Behandle SøknadBehandlet`() {
        mediator.behandle(
            SøknadBehandlet(
                behandlingId = oppgaveId,
                innvilget = true,
            ),
        )

        testRapid.inspektør.size shouldBe 1
        val event = testRapid.inspektør.message(0)
        val partitionKey = testRapid.inspektør.key(0)

        println(event)

        partitionKey shouldBe ident
        event["@event_name"].asText() shouldBe "søknad_behandlet_hendelse"
    }

    private val mockOppgaveRepository = InMemoryOppgaveRepository().apply {
        oppgave = Oppgave(
            behandling(testPerson) {
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
