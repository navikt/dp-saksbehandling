package no.nav.dagpenger.saksbehandling.db.sak

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.NyBehandling
import no.nav.dagpenger.saksbehandling.NyPerson
import no.nav.dagpenger.saksbehandling.NySak
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PostgresRepositoryTest {
    private val nå = LocalDateTime.now()

    private val behandling =
        NyBehandling(
            behandlingId = UUIDv7.ny(),
            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
        )
    private val sak =
        NySak(
            søknadId = UUIDv7.ny(),
            opprettet = nå,
        ).also {
            it.leggTilBehandling(
                behandling,
            )
        }
    private val nyPerson =
        NyPerson(
            ident = "12345678901",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = UGRADERT,
        ).also { it.leggTilSak(sak) }

    @Test
    fun `Skal kunne lagre en person`() {
        withMigratedDb {
            val personRepository = PostgresRepository(dataSource = dataSource)

            personRepository.lagre(nyPerson)

            personRepository.hent(nyPerson.ident) shouldBe nyPerson
        }
    }
}
