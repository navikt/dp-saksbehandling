package no.nav.dagpenger.behandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSak
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.testBehandling
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.helpers.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PostgresOppgaveRepositoryTest {
    private object TestData {
        val testBehandling: Behandling = Behandling(
            person = testPerson,
            hendelse = testHendelse,
            steg = testSteg,
            sak = testSak,
        )

        val testSteg: Set<Steg<*>>
            get() {
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

                val stegE = Steg.fastsettelse<Double>("E")

                val vilkårF = Steg.Vilkår(id = "F").also {
                    it.avhengerAv(stegE)
                }

                return setOf(fastsettelseA, fastsettelseB, fastsettelseC, fastsettelseD, stegE, vilkårF)
            }
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
                    rehydrertBehandling.alleSteg().first { it.id == "A" }.alleSteg().map { it.id } shouldBe
                        testBehandling.alleSteg().first { it.id == "A" }.alleSteg().map { it.id }

                    rehydrertBehandling.alleSteg().first { it.id == "F" }.alleSteg().map { it.id } shouldBe
                        testBehandling.alleSteg().first { it.id == "F" }.alleSteg().map { it.id }
//                    rehydrertBehandling.behandler shouldBe testBehandling.behandler
//                    rehydrertBehandling.sak shouldBe testBehandling.sak
                }
            }
        }
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
