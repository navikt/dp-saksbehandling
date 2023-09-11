package no.nav.dagpenger.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testIdent
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSporing
import no.nav.dagpenger.behandling.Tilstand.Utført
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.hendelser.VedtakStansetHendelse
import no.nav.dagpenger.behandling.oppgave.InMemoryOppgaveRepository
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class MediatorTest {
    private val testRapid = TestRapid()
    private val ident = testIdent
    private val sakId = UUID.randomUUID()
    private var oppgave: Oppgave
    private var oppgaveId: UUID

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun UtførStegKommando() {
        mediator.utfør(
            UtførStegKommando(oppgaveId, Saksbehandler(ident), "") {
                besvar(finnStegId("vilkår1"), false, it)
            },
        )

        mediator.utfør(
            UtførStegKommando(oppgaveId, Saksbehandler(ident), "") {
                besvar(finnStegId("vilkår 1 dato"), LocalDate.now(), it)
            },
        )

        mediator.utfør(
            UtførStegKommando(oppgaveId, Saksbehandler(ident), "") {
                besvar(finnStegId("fastsettelse1"), 2, it)
            },
        )
    }

    @Test
    fun `VedtakFattet event skal publisere melding på rapiden`() {
        val behandlingId = UUID.randomUUID()
        val sakId = UUID.randomUUID()

        mediator.vedtakFattet(
            BehandlingObserver.VedtakFattet(
                behandlingId = behandlingId,
                ident = testIdent,
                utfall = Utfall.Avslag,
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
    fun `Alle nye oppgaver havner på samme sak (Viggo case)`() {
        val ident = "88888888888"
        mediator.behandle(SøknadInnsendtHendelse(UUID.randomUUID(), "123", ident))
        mediator.behandle(SøknadInnsendtHendelse(UUID.randomUUID(), "123", ident))

        mockOppgaveRepository.hentOppgaverFor(ident).let {
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

        override fun visit(oppgaveId: UUID, opprettet: LocalDateTime, utføresAv: Saksbehandler?) {
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
    fun `Publiserer melding rettighet_behandlet_hendelse når behandlingen er ferdig`() {
        val hendelse = SøknadInnsendtHendelse(UUID.randomUUID(), "123", testIdent)
        mediator.behandle(hendelse)
        val oppgaveId = mockOppgaveRepository.hentOppgaver().last().uuid
        val oppgave = mockOppgaveRepository.hentOppgave(oppgaveId)
        mediator.utfør(
            UtførStegKommando(oppgaveId, Saksbehandler(ident), "") {
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
            },
        )

        testRapid.inspektør.size shouldBe 2
        val event = testRapid.inspektør.message(0)
        val partitionKey = testRapid.inspektør.key(0)

        partitionKey shouldBe ident
        event["@event_name"].asText() shouldBe "rettighet_behandlet_hendelse"
    }

    @Test
    fun `VedtakStansetHendelse fører til en ny oppgave med behandling for stans`() {
        val ukjentIdent = "22222222222"
        shouldThrow<IllegalArgumentException> {
            mediator.behandle(VedtakStansetHendelse(ukjentIdent))
        }

        mockOppgaveRepository.hentOppgaverFor(testIdent).size shouldBe 2
        mediator.behandle(VedtakStansetHendelse(testIdent))
        mockOppgaveRepository.hentOppgaverFor(testIdent).size shouldBe 3
    }

    private val mockOppgaveRepository = InMemoryOppgaveRepository().apply {
        oppgave = Oppgave(
            UUID.randomUUID(),
            behandling(testPerson, testHendelse, Sak(sakId)) {
                steg {
                    vilkår("vilkår1") {
                        avhengerAvFastsettelse<LocalDate>("vilkår 1 dato")
                    }
                }
                steg {
                    fastsettelse<Int>("fastsettelse1")
                }
            },
        )
        oppgaveId = oppgave.uuid
        lagreOppgave(oppgave)
        val søknadInnsendtHendelse = SøknadInnsendtHendelse(UUID.randomUUID(), "", testIdent)
        lagreOppgave(
            søknadInnsendtHendelse.oppgave(
                testPerson.also {
                    it.håndter(søknadInnsendtHendelse)
                },
            ),
        )
    }

    private val mockPersonRepository = InMemoryPersonRepository.apply {
        lagrePerson(testPerson)
    }

    private val mediator = Mediator(
        rapidsConnection = testRapid,
        oppgaveRepository = mockOppgaveRepository,
        personRepository = mockPersonRepository,
        aktivitetsloggMediator = mockk(relaxed = true),
    )

    private fun finnStegId(id: String) = oppgave.alleSteg().single { it.id == id }.uuid
}
