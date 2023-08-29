package no.nav.dagpenger.behandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSak
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.helpers.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test

class PostgresOppgaveRepositoryTest {

    @Test
    fun `lagring og henting av behandling`() {
        withMigratedDb {
            PostgresRepository(dataSource).let { repository ->
                repository.lagrePerson(testPerson)
            }
            PostgresOppgaveRepository(dataSource).let { repository ->
                shouldNotThrowAny {
                    repository.lagreBehandling(testBehandling())
                }
            }
        }
    }

    private fun testBehandling(): Behandling {
        return Behandling(
            person = testPerson,
            hendelse = testHendelse,
            steg = testSteg(),
            sak = testSak,
        )
    }

    private fun testSteg(): Set<Steg<*>> {
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
