package no.nav.dagpenger.saksbehandling.db.sak

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.NyBehandling
import no.nav.dagpenger.saksbehandling.NyPerson
import no.nav.dagpenger.saksbehandling.NySak
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource

class PostgresRepositoryTest {
    private val nå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

    private val oppgaveId = UUIDv7.ny()
    private val behandling1 =
        NyBehandling(
            behandlingId = UUIDv7.ny(),
            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
            oppgaveId = oppgaveId,
        )
    private val behandling2 =
        NyBehandling(
            behandlingId = UUIDv7.ny(),
            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
        )
    private val behandling3 =
        NyBehandling(
            behandlingId = UUIDv7.ny(),
            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
        )
    private val behandling4 =
        NyBehandling(
            behandlingId = UUIDv7.ny(),
            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
        )
    private val sak1 =
        NySak(
            søknadId = UUIDv7.ny(),
            opprettet = nå,
        ).also {
            it.leggTilBehandling(behandling1)
            it.leggTilBehandling(behandling2)
        }
    private val sak2 =
        NySak(
            søknadId = UUIDv7.ny(),
            opprettet = nå,
        ).also {
            it.leggTilBehandling(behandling3)
            it.leggTilBehandling(behandling4)
        }
    private val nyPerson =
        NyPerson(
            ident = "12345678901",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = UGRADERT,
        ).also {
            it.leggTilSak(sak1)
            it.leggTilSak(sak2)
        }

    @Test
    fun `Skal kunne lagre en person`() {
        withMigratedDb {
            val personRepository = PostgresRepository(dataSource = dataSource)
            personRepository.lagre(nyPerson)
            dataSource.insertOppgaveRad(oppgaveId, behandling1.behandlingId)

            val personFraDB = personRepository.hent(nyPerson.ident)

            personFraDB shouldBe nyPerson
        }
    }

    private fun DataSource.insertOppgaveRad(
        oppgaveId: UUID,
        behandlingId: UUID,
    ) {
        sessionOf(this).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO oppgave_v1
                            (id, behandling_id, tilstand, opprettet)
                        VALUES
                            (:id, :behandling_id, :tilstand, :opprettet) 
                        ON CONFLICT(id) DO UPDATE SET
                         tilstand = :tilstand,
                         saksbehandler_ident = :saksbehandler_ident,
                         utsatt_til = :utsatt_til
                        
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "id" to oppgaveId,
                            "behandling_id" to behandlingId,
                            "tilstand" to Oppgave.Tilstand.Type.OPPRETTET.name,
                            "opprettet" to nå,
                        ),
                ).asUpdate,
            )
        }
    }
}
