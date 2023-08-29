package no.nav.dagpenger.behandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSak
import no.nav.dagpenger.behandling.PostgresOppgaveRepositoryTest.TestData.testBehandling
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.helpers.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test

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
                val stegC = Steg.Vilkår("C")
                val stegB = Steg.Vilkår("B").also {
                    it.avhengerAv(stegC)
                }
                val stegA = Steg.Vilkår("A").also {
                    it.avhengerAv(stegB)
                }
                val stegD = Steg.Vilkår("D").also {
                    it.avhengerAv(stegC)
                }
                return setOf(stegA, stegB, stegC, stegD)
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
//                    rehydrertBehandling.steg shouldBe testBehandling.steg
                    rehydrertBehandling.opprettet shouldBe testBehandling.opprettet
                    rehydrertBehandling.uuid shouldBe testBehandling.uuid
                    rehydrertBehandling.tilstand shouldBe testBehandling.tilstand
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
