package no.nav.dagpenger.behandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSak
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.fastsettelseA
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.fastsettelseB
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.fastsettelseC
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.fastsettelseE
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.testBehandling
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.vilkårF
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.helpers.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PostgresOppgaveRepositoryTest {
    private object TestData {
        val fastsettelseC = Steg.fastsettelse<String>("C")
        val fastsettelseB = Steg.fastsettelse<Int>("B").also {
            it.avhengerAv(fastsettelseC)
        }
        val fastsettelseA = Steg.fastsettelse<LocalDate>("A").also {
            it.avhengerAv(fastsettelseB)
        }
        val fastsettelseD = Steg.fastsettelse<Boolean>("D").also {
            it.avhengerAv(fastsettelseC)
        }

        val fastsettelseE = Steg.fastsettelse<Double>("E")

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
                    // todo better test: Check more properties
                    rehydrertBehandling.alleSteg() shouldContainExactly testBehandling.alleSteg()

                    // todo refactor. Check children/avhengerAv relasjon
                    rehydrertBehandling.getStegById("A").avhengigeSteg() shouldBe setOf(fastsettelseB)
                    rehydrertBehandling.getStegById("B").avhengigeSteg() shouldBe setOf(fastsettelseC)
                    rehydrertBehandling.getStegById("D").avhengigeSteg() shouldBe setOf(fastsettelseC)
                    rehydrertBehandling.getStegById("F").avhengigeSteg() shouldBe setOf(fastsettelseC, fastsettelseE)

                    // Check rekursivt avengige stege
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
