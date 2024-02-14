package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Meldingsfabrikk.søknadInnsendtHendelse
import no.nav.dagpenger.saksbehandling.Meldingsfabrikk.testIdent
import no.nav.dagpenger.saksbehandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.saksbehandling.db.InMemoryOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.InMemoryPersonRepository
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.saksbehandling.db.PostgresRepository
import no.nav.dagpenger.saksbehandling.dsl.BehandlingDSL.Companion.behandling
import no.nav.dagpenger.saksbehandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakStansetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VurderAvslagPåMinsteinntektHendelse
import no.nav.dagpenger.saksbehandling.oppgave.Oppgave
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Disabled
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
    fun `UtførStegKommando kan utføre steg`() {
        mediator.utfør(
            UtførStegKommando(
                oppgaveId,
                Saksbehandler(ident),
                "",
                "",
                "token",
            ) {
                besvar(finnStegId("vilkår1"), false, it)
            },
        )

        mediator.utfør(
            UtførStegKommando(
                oppgaveId,
                Saksbehandler(ident),
                "",
                "",
                "token",
            ) {
                besvar(finnStegId("vilkår 1 dato"), LocalDate.now(), it)
            },
        )

        mediator.utfør(
            UtførStegKommando(
                oppgaveId,
                Saksbehandler(ident),
                "",
                "",
                "token",
            ) {
                besvar(finnStegId("fastsettelse1"), 2, it)
            },
        )
    }

    @Test
    fun `Alle nye oppgaver havner på samme sak (Viggo case)`() {
        val ident = "88888888888"
        mediator.behandle(
            SøknadInnsendtHendelse(
                søknadId = UUID.randomUUID(),
                journalpostId = "123",
                ident = ident,
                innsendtDato = LocalDate.MIN,
            ),
        )
        mediator.behandle(
            SøknadInnsendtHendelse(
                søknadId = UUID.randomUUID(),
                journalpostId = "123",
                ident = ident,
                innsendtDato = LocalDate.MIN,
            ),
        )

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
        lateinit var emneknagger: Set<String>

        init {
            oppgave.accept(this)
        }

        override fun visit(
            behandlingId: UUID,
            sak: Sak,
        ) {
            this.behandlingId = behandlingId
            this.sak = sak
        }

        override fun visit(
            oppgaveUUID: UUID,
            opprettet: LocalDateTime,
            utføresAv: Saksbehandler?,
            emneknagger: Set<String>,
        ) {
            this.oppgaveId = oppgaveUUID
            this.emneknagger = emneknagger
        }
    }

    @Test
    fun `Behandle SøknadInnsendtHendelse`() {
        mediator.behandle(
            SøknadInnsendtHendelse(
                søknadId = UUID.randomUUID(),
                journalpostId = "123",
                ident = testIdent,
                innsendtDato = LocalDate.MIN,
            ),
        )
        InMemoryPersonRepository.hentPerson(testIdent).also {
            it shouldNotBe null
            it!!.hentGjeldendeSak() shouldNotBe null
        }
    }

    @Test
    fun `Behandle VurderAvslagPåMinsteinntektHendelse`() =
        withMigratedDb {
            val postgresRepository = PostgresRepository(dataSource)
            val mediator = mediatorMedDb(postgresRepository)

            val søknadId = UUID.randomUUID()

            mediator.behandle(
                SøknadInnsendtHendelse(
                    søknadId = søknadId,
                    journalpostId = "123",
                    ident = testIdent,
                    innsendtDato = LocalDate.now(),
                ),
            )

            TestVisitor(postgresRepository.hentOppgaveFor(søknadId)).emneknagger shouldBe
                setOf(
                    "Søknadsbehandling",
                )

            mediator.behandle(
                VurderAvslagPåMinsteinntektHendelse(
                    ident = testIdent,
                    søknadUUID = søknadId,
                    meldingsreferanseId = søknadId,
                ),
            )

            TestVisitor(postgresRepository.hentOppgaveFor(søknadId)).emneknagger shouldBe
                setOf(
                    "Søknadsbehandling",
                    "VurderAvslagPåMinsteinntekt",
                )
        }

    private fun mediatorMedDb(postgresRepository: PostgresRepository) =
        Mediator(
            rapidsConnection = testRapid,
            oppgaveRepository = postgresRepository,
            personRepository = postgresRepository,
            aktivitetsloggMediator = mockk(relaxed = true),
            vurderingRepository = mockk(relaxed = true),
        )

    @Test
    fun `VedtakStansetHendelse fører til en ny oppgave med behandling for stans`() {
        val ukjentIdent = "22222222222"
        shouldThrow<IllegalArgumentException> {
            mediator.behandle(VedtakStansetHendelse(ident = ukjentIdent))
        }

        mockOppgaveRepository.hentOppgaverFor(testIdent).size shouldBe 2
        mediator.behandle(VedtakStansetHendelse(ident = testIdent))
        mockOppgaveRepository.hentOppgaverFor(testIdent).size shouldBe 3
    }

    val behandling =
        behandling(testPerson, søknadInnsendtHendelse, Sak(sakId)) {
            steg {
                vilkår("vilkår1") {
                    avhengerAvFastsettelse<LocalDate>("vilkår 1 dato")
                }
            }
            steg {
                fastsettelse<Int>("fastsettelse1")
            }
        }
    private val mockOppgaveRepository =
        InMemoryOppgaveRepository().apply {
            oppgave =
                Oppgave(
                    UUID.randomUUID(),
                    behandling,
                )
            oppgaveId = oppgave.uuid
            lagreOppgave(oppgave)
            val søknadInnsendtHendelse =
                SøknadInnsendtHendelse(
                    søknadId = UUID.randomUUID(),
                    journalpostId = "",
                    ident = testIdent,
                    innsendtDato = LocalDate.MIN,
                )
            lagreOppgave(
                søknadInnsendtHendelse.oppgave(
                    testPerson.also {
                        it.håndter(søknadInnsendtHendelse)
                    },
                ),
            )
        }

    private val mockPersonRepository =
        InMemoryPersonRepository.apply {
            lagrePerson(testPerson)
        }

    private val mediator =
        Mediator(
            rapidsConnection = testRapid,
            oppgaveRepository = mockOppgaveRepository,
            personRepository = mockPersonRepository,
            aktivitetsloggMediator = mockk(relaxed = true),
            vurderingRepository = mockk(relaxed = true),
        )

    private fun finnStegId(id: String) = oppgave.alleSteg().single { it.id == id }.uuid
}
