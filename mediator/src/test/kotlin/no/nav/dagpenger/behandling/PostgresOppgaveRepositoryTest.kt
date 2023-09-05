package no.nav.dagpenger.behandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
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
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.testBehandling
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.testOppgave
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.vilkårF
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.helpers.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.oppgave.Oppgave
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PostgresOppgaveRepositoryTest {
    private object TestData {
        val enString = "fastsettelseC"
        val fastsettelseC = Steg.fastsettelse<String>("C").also {
            it.besvar(enString, NullSporing()) // todo sporing?
        }
        val etHeltall = 1
        val fastsettelseB = Steg.fastsettelse<Int>("B").also {
            it.avhengerAv(fastsettelseC)
            it.besvar(etHeltall, NullSporing())
        }
        val enDato: LocalDate = LocalDate.of(2022, 2, 2)
        val fastsettelseA = Steg.fastsettelse<LocalDate>("A").also {
            it.avhengerAv(fastsettelseB)
            it.besvar(enDato, NullSporing())
        }
        val enBoolean = true
        val fastsettelseD = Steg.fastsettelse<Boolean>("D").also {
            it.avhengerAv(fastsettelseC)
            it.besvar(enBoolean, NullSporing())
        }

        val etDesimaltall = 2.0
        val fastsettelseE = Steg.fastsettelse<Double>("E").also {
            it.besvar(etDesimaltall, NullSporing())
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
                    rehydrertBehandling.opprettet shouldBe testBehandling.opprettet
                    rehydrertBehandling.uuid shouldBe testBehandling.uuid
                    rehydrertBehandling.tilstand shouldBe testBehandling.tilstand
                    // todo check behandler
//                    rehydrertBehandling.behandler shouldBe something

                    rehydrertBehandling.sak shouldBe testSak

                    // Steg
                    rehydrertBehandling.alleSteg() shouldContainExactly testBehandling.alleSteg()
                    rehydrertBehandling.getStegById("A").svar.verdi shouldBe enDato
                    rehydrertBehandling.getStegById("B").svar.verdi shouldBe etHeltall
                    rehydrertBehandling.getStegById("C").svar.verdi shouldBe enString
                    rehydrertBehandling.getStegById("D").svar.verdi shouldBe enBoolean
                    rehydrertBehandling.getStegById("E").svar.verdi shouldBe etDesimaltall

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
                    oppgave.opprettet shouldBe testOppgave.opprettet
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
                    oppgave.opprettet shouldBe testOppgave.opprettet
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
