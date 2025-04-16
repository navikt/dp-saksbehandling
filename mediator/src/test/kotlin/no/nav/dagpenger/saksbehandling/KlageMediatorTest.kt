package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.klage.InmemoryKlageRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KlageMediatorTest {
    private val testPersonIdent = "12345678901"

    private val oppslagMock =
        mockk<Oppslag>().also {
            coEvery { it.hentPersonMedSkjermingOgGradering(testPersonIdent) } returns
                Person(
                    id = UUIDv7.ny(),
                    ident = testPersonIdent,
                    skjermesSomEgneAnsatte = false,
                    adressebeskyttelseGradering = UGRADERT,
                )
        }

    @Test
    fun `livsyklus til en klage`() {
        withMigratedDb { ds ->
            val klageMediator =
                KlageMediator(
                    klageRepository = InmemoryKlageRepository,
                    personRepository = PostgresPersonRepository(ds),
                    oppslag = oppslagMock,
                )

            klageMediator.opprettKlage(
                KlageMottattHendelse(
                    ident = testPersonIdent,
                    opprettet = LocalDateTime.now(),
                    journalpostId = "journalpostId",
                ),
            )

            val klageOppgave =
                InmemoryKlageRepository.hentKlager().single {
                    it.behandling.person.ident == testPersonIdent
                }

            klageOppgave shouldNotBe null
        }
    }
}
