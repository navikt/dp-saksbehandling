package no.nav.dagpenger.behandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.date.shouldBeWithin
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSak
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.enBoolean
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.enDato
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.enString
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.etDesimaltall
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.etHeltall
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.fastsettelseA
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.fastsettelseB
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.fastsettelseC
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.fastsettelseE
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.manuellSporing
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.quizSporing
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.testBehandling
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.testOppgave
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.vilkårF
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.helpers.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

class PostgresOppgaveRepositoryTest {

    private object TestData {
        val now: LocalDateTime = LocalDateTime.now()
        val enString = "fastsettelseC"
        val manuellSporing = ManuellSporing(
            utført = now,
            utførtAv = Saksbehandler(ident = "saksbehandlinger"),
            begrunnelse = "",

        )

        @Language("JSON")
        val quizSporing = QuizSporing(
            utført = now,
            json = """{}""",
        )

        val fastsettelseC = Steg.fastsettelse<String>("C").also {
            it.besvar(enString, manuellSporing)
        }
        val etHeltall = 1
        val fastsettelseB = Steg.fastsettelse<Int>("B").also {
            it.avhengerAv(fastsettelseC)
            it.besvar(etHeltall, manuellSporing)
        }
        val enDato: LocalDate = LocalDate.of(2022, 2, 2)
        val fastsettelseA = Steg.fastsettelse<LocalDate>("A").also {
            it.avhengerAv(fastsettelseB)
            it.besvar(enDato, quizSporing)
        }
        val enBoolean = true
        val fastsettelseD = Steg.fastsettelse<Boolean>("D").also {
            it.avhengerAv(fastsettelseC)
            it.besvar(enBoolean, ManuellSporing(LocalDateTime.now(), Saksbehandler("123"), ""))
        }

        val etDesimaltall = 2.0
        val fastsettelseE = Steg.fastsettelse<Double>("E").also {
            it.besvar(etDesimaltall, ManuellSporing(LocalDateTime.now(), Saksbehandler("123"), ""))
        }

        val vilkårF = Steg.Vilkår(id = "F").also {
            it.avhengerAv(fastsettelseE)
            it.avhengerAv(fastsettelseC)
        }

        val testSteg: Set<Steg<*>> =
            setOf(fastsettelseA, fastsettelseB, fastsettelseC, fastsettelseD, fastsettelseE, vilkårF)

        val testBehandling: Behandling = Behandling(
            person = testPerson,
            hendelse = testHendelse,
            steg = testSteg,
            sak = testSak,
        )
        val testOppgave = Oppgave(testBehandling)
    }

    @Test
    fun `lagring og henting av behandling`() {
        withMigratedDb {
            PostgresRepository(dataSource).let { repository ->
                repository.lagrePerson(testPerson)
                shouldNotThrowAny {
                    repository.lagreBehandling(testBehandling)
                }

                repository.hentBehandling(testBehandling.uuid).let { rehydrertBehandling ->
                    rehydrertBehandling.person shouldBe testBehandling.person
                    rehydrertBehandling.opprettet.shouldBeWithin(
                        1.milliseconds.toJavaDuration(),
                        testBehandling.opprettet,
                    )
                    rehydrertBehandling.uuid shouldBe testBehandling.uuid
                    rehydrertBehandling.tilstand shouldBe testBehandling.tilstand
                    // todo check behandler
//                    rehydrertBehandling.behandler shouldBe something

                    rehydrertBehandling.sak shouldBe testSak

                    // Steg
                    rehydrertBehandling.alleSteg() shouldContainExactly testBehandling.alleSteg()
                    // Check children/avhengerAv relasjon
                    rehydrertBehandling.getStegById("A").avhengigeSteg() shouldBe setOf(fastsettelseB)
                    rehydrertBehandling.getStegById("B").avhengigeSteg() shouldBe setOf(fastsettelseC)
                    rehydrertBehandling.getStegById("D").avhengigeSteg() shouldBe setOf(fastsettelseC)
                    rehydrertBehandling.getStegById("F").avhengigeSteg() shouldBe setOf(fastsettelseC, fastsettelseE)
                    // Check rekursivt avengige steg
                    rehydrertBehandling.getStegById("A").alleSteg() shouldBe setOf(
                        fastsettelseA,
                        fastsettelseB,
                        fastsettelseC,
                    )
                    rehydrertBehandling.getStegById("F").alleSteg() shouldBe setOf(
                        vilkårF,
                        fastsettelseC,
                        fastsettelseE,
                    )

                    // Svar
                    rehydrertBehandling.getStegById("A").svar.let { svar ->
                        svar.verdi shouldBe enDato
                        svar.sporing.shouldBeTypeOf<QuizSporing>()
                        svar.sporing.utført.shouldBeWithin(1.milliseconds.toJavaDuration(), quizSporing.utført)
                        (svar.sporing as QuizSporing).json shouldBe quizSporing.json
                    }
                    rehydrertBehandling.getStegById("B").svar.let { svar ->
                        svar.verdi shouldBe etHeltall
                        svar.sporing.shouldBeTypeOf<ManuellSporing>()
                        svar.sporing.utført shouldBe manuellSporing.utført
                        (svar.sporing as ManuellSporing).utførtAv shouldBe manuellSporing.utførtAv
                    }
                    rehydrertBehandling.getStegById("C").svar.let { svar ->
                        svar.verdi shouldBe enString
                        svar.sporing.shouldBeTypeOf<ManuellSporing>()
                        svar.sporing.utført shouldBe manuellSporing.utført
                        (svar.sporing as ManuellSporing).utførtAv shouldBe manuellSporing.utførtAv
                    }
                    rehydrertBehandling.getStegById("D").svar.verdi shouldBe enBoolean
                    rehydrertBehandling.getStegById("E").svar.verdi shouldBe etDesimaltall
                }
            }
        }
    }

    @Test
    fun `lagring og henting av oppgave`() {
        withMigratedDb {
            PostgresRepository(dataSource).let { repository ->
                repository.lagrePerson(testPerson)
                shouldNotThrowAny {
                    repository.lagreOppgave(testOppgave)
                }

                repository.hentOppgave(testOppgave.uuid).let { oppgave ->
                    oppgave.uuid shouldBe testOppgave.uuid
                    oppgave.utføresAv shouldBe testOppgave.utføresAv
                    oppgave.person shouldBe testOppgave.person
                    oppgave.opprettet.shouldBeWithin(1.milliseconds.toJavaDuration(), testOppgave.opprettet)
                    oppgave.tilstand shouldBe testOppgave.tilstand
                }
            }
        }
    }

    @Test
    fun `lagring og henting av oppgaver`() {
        withMigratedDb {
            PostgresRepository(dataSource).let { repository ->
                repository.lagrePerson(testPerson)
                shouldNotThrowAny {
                    repository.lagreOppgave(testOppgave)
                }

                repository.hentOppgaverFor(testOppgave.person.ident).first().let { oppgave ->
                    oppgave.uuid shouldBe testOppgave.uuid
                    oppgave.utføresAv shouldBe testOppgave.utføresAv
                    oppgave.person shouldBe testOppgave.person
                    oppgave.opprettet.shouldBeWithin(1.milliseconds.toJavaDuration(), testOppgave.opprettet)
                    oppgave.tilstand shouldBe testOppgave.tilstand
                }
            }
        }
    }

    private fun Behandling.getStegById(id: String): Steg<*> {
        return this.alleSteg().first { it.id == id }
    }

    @Test
    fun `Kan oppdatere en eksisterende behandling`() {
        withMigratedDb {
            PostgresRepository(dataSource).let { repository ->
                repository.lagrePerson(testPerson)
                shouldNotThrowAny {
                    repository.lagreBehandling(testBehandling)
                    repository.lagreBehandling(testBehandling)
                    // todo finne en måte å teste det som kan muteres
                }
            }
        }
    }
}
