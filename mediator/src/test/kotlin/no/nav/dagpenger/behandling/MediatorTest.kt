package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
    fun `VedtakFattet event skal publisere melding på rapiden`() {
        val behandlingId = UUID.randomUUID()
        val sakId = UUID.randomUUID()

        mediator.vedtakFattet(
            BehandlingObserver.VedtakFattet(
                behandlingId = behandlingId,
                ident = testIdent,
                utfall = false,
                fastsettelser = mapOf("f1" to "f2"),
                sakId = sakId,
            ),
        )

        testRapid.inspektør.message(0).let {
            it["behandlingId"].asText() shouldBe behandlingId.toString()
            it["ident"].asText() shouldBe testIdent
            it["utfall"].asBoolean() shouldBe false
            it["f1"].asText() shouldBe "f2"
            it["sakId"].asText() shouldBe sakId.toString()
        }
    }

    @Test
    fun `Flere SøknadInnsendtHendelser fører bare til én sak (Viggo case)`() {
        mediator.behandle(SøknadInnsendtHendelse(UUID.randomUUID(), "123", "88888888888"))
        mediator.behandle(SøknadInnsendtHendelse(UUID.randomUUID(), "123", "88888888888"))

        mockOppgaveRepository.hentOppgaverFor("88888888888").let {
            it.size shouldBe 2
            val oppgaveVisitor1 = TestVisitor(it.first())
            val oppgaveVisitor2 = TestVisitor(it.last())

            oppgaveVisitor1.sak.id shouldBe oppgaveVisitor2.sak.id
            oppgaveVisitor1.oppgaveId shouldNotBe oppgaveVisitor2.oppgaveId
            oppgaveVisitor1.behandlingId shouldNotBe oppgaveVisitor2.behandlingId
        }
    }

    private class TestVisitor(oppgave: Oppgave) : OppgaveVisitor {
        lateinit var sak: Sak
        lateinit var behandlingId: UUID
        lateinit var oppgaveId: UUID

        init {
            oppgave.accept(this)
        }

        override fun visit(behandlingId: UUID, sak: Sak) {
            this.behandlingId = behandlingId
            this.sak = sak
        }

        override fun visit(oppgaveId: UUID) {
            this.oppgaveId = oppgaveId
        }
    }

    @Test
    fun `Behandle SøknadInnsendtHendelse`() {
        mediator.behandle(SøknadInnsendtHendelse(UUID.randomUUID(), "123", testIdent))
        InMemoryPersonRepository.hentPerson(testIdent).also {
            it shouldNotBe null
            it!!.hentGjeldendeSak() shouldNotBe null
        }
    }

    @Test
    fun `Behandle SøknadBehandlet`() {
        val hendelse = SøknadInnsendtHendelse(UUID.randomUUID(), "123", testIdent)
        mediator.behandle(hendelse)
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
            behandling(testPerson, testHendelse, Sak()) {
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
        val søknadInnsendtHendelse = SøknadInnsendtHendelse(UUID.randomUUID(), "", "20987654321")
        lagreOppgave(
            søknadInnsendtHendelse.oppgave(
                Person("20987654321").also {
                    it.håndter(søknadInnsendtHendelse)
                },
            ),
        )
    }

    private val mediator = Mediator(
        rapidsConnection = testRapid,
        oppgaveRepository = mockOppgaveRepository,
    )

    private fun finnStegId(id: String) = oppgave.alleSteg().single { it.id == id }.uuid
}
