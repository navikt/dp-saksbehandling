package no.nav.dagpenger.behandling.db

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.PersonVisitor
import no.nav.dagpenger.behandling.PostgresRepository
import no.nav.dagpenger.behandling.Sak
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.helpers.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class PostgresPersonRepositoryTest {
    private val testIdent = "12345678910"

    @Test
    fun `lagring og henting av person med saker`() {
        withMigratedDb {
            val repository = PostgresRepository(dataSource)
            Person(testIdent).also {
                it.håndter(
                    SøknadInnsendtHendelse(
                        UUID.randomUUID(),
                        "123",
                        "123",
                    ),
                )
                repository.lagrePerson(it)
            }

            repository.hentPerson(testIdent).let { lagretPerson ->
                lagretPerson shouldNotBe null
                lagretPerson!!.ident shouldBe testIdent

                TestPersonVisitor(lagretPerson).saker.let { saker ->
                    saker.size shouldBe 1
                }
            }
        }
    }

    class TestPersonVisitor(person: Person) : PersonVisitor {
        lateinit var saker: Set<Sak>

        init {
            person.accept(this)
        }

        override fun visit(saker: Set<Sak>) {
            this.saker = saker
        }
    }
}
