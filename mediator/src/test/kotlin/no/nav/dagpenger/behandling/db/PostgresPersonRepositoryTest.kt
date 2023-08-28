package no.nav.dagpenger.behandling.db

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.PostgresPersonRepository
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.helpers.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test

class PostgresPersonRepositoryTest {
    private val testIdent = "12345678910"

    @Test
    fun `lagring og henter en komplett person`() {
        withMigratedDb {
            val repository = PostgresPersonRepository(dataSource)
            Person(testIdent).also { repository.lagrePerson(it) }

            repository.hentPerson(testIdent).let { lagretPerson ->
                lagretPerson shouldNotBe null
                lagretPerson!!.ident shouldBe testIdent
            }
        }
    }
}
