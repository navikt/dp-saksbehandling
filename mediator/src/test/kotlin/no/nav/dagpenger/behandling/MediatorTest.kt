package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Meldingsfabrikk.testIdent
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.dsl.BehandlingDSL
import no.nav.dagpenger.behandling.hendelser.BehandlingSvar
import no.nav.dagpenger.behandling.oppgave.InMemoryOppgaveRepository
import no.nav.dagpenger.behandling.persistence.BehandlingRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class MediatorTest {
    private val testRapid = TestRapid()
    private lateinit var mediator: Mediator
    private val ident = testIdent

    @BeforeEach
    fun setup() {
        mediator = Mediator(
            rapidsConnection = testRapid,
            behandlingRepository = mockPersistence,
            oppgaveRepository = mockOppgaveRepository,
        )
    }

    @Test
    fun `Behandle BehandlingSvar hendelse`() {
        mediator.behandle(
            BehandlingSvar(
                ident = ident,
                behandlingUUID = mockPersistence.behandlingId,
                stegUUID = mockPersistence.finnStegId("vilkår1"),
                verdi = false,
            ),
        )

        mediator.behandle(
            BehandlingSvar(
                ident = ident,
                behandlingUUID = mockPersistence.behandlingId,
                stegUUID = mockPersistence.finnStegId("vilkår 1 dato"),
                verdi = LocalDate.now(),
            ),
        )

        mediator.behandle(
            BehandlingSvar(
                ident = ident,
                behandlingUUID = mockPersistence.behandlingId,
                stegUUID = mockPersistence.finnStegId("fastsettelse1"),
                verdi = 3,
            ),
        )
    }

    @Test
    fun `Behandle SøknadBehandlet`() {
        mediator.behandle(
            SøknadBehandlet(
                behandlingId = mockPersistence.behandlingId,
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

    private val mockOppgaveRepository = InMemoryOppgaveRepository()
    private val mockPersistence = object : BehandlingRepository {
        var behandlingId: UUID
        val behandlinger = listOf(
            BehandlingDSL.behandling(testPerson) {
                steg {
                    vilkår("vilkår1") {
                        avhengerAvFastsettelse<LocalDate>("vilkår 1 dato")
                    }
                }
                steg {
                    fastsettelse<Int>("fastsettelse1")
                }
            }.also { behandlingId = it.uuid },
        )

        override fun hentBehandlinger() = behandlinger
        override fun hentBehandling(behandlingUUID: UUID) = behandlinger.single { it.uuid == behandlingUUID }
        override fun hentBehandlingerFor(fnr: String): List<Behandling> {
            TODO("Not yet implemented")
        }

        fun finnStegId(id: String): UUID {
            return behandlinger.single().alleSteg().single { it.id == id }.uuid
        }
    }
}
