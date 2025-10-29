package no.nav.dagpenger.saksbehandling.db

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.lagBehandling
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.util.UUID
import javax.sql.DataSource

class PostgresTriggerTest {
    @Test
    fun `Når en oppgave endres så skal endret_tidspunkt oppdateres`() {
        val testPerson =
            Person(
                ident = "12345678901",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            )
        val oppgaveId = UUIDv7.ny()
        val behandling = lagBehandling()
//            Behandling(
//            behandlingId = UUIDv7.ny(),
//            opprettet = LocalDateTime.now(),
//            hendelse = TomHendelse,
//            utløstAv = UtløstAvType.SØKNAD,
//            oppgaveId = oppgaveId
//        )
        val testOppgave =
            Oppgave(
                oppgaveId = oppgaveId,
                emneknagger = setOf("Hugga", "Bugga"),
                opprettet = behandling.opprettet,
                tilstand = Oppgave.KlarTilBehandling,
                behandling = behandling,
                person = testPerson,
                meldingOmVedtak =
                    Oppgave.MeldingOmVedtak(
                        kilde = DP_SAK,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                    ),
            )
        DBTestHelper.withBehandling(person = testPerson, behandling = behandling) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)
            val endretTidspunkt = ds.hentEndretTidspunkt(testOppgave.oppgaveId)
            Thread.sleep(100)

            val endretOppgave = testOppgave.copy(tilstand = Oppgave.UnderBehandling)
            repo.lagre(endretOppgave)
            val nyttEndretTidspunkt = ds.hentEndretTidspunkt(testOppgave.oppgaveId)
            nyttEndretTidspunkt.after(endretTidspunkt) shouldBe true
        }
    }
}

private fun DataSource.hentEndretTidspunkt(oppgaveId: UUID): Timestamp {
    return sessionOf(this).use { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    SELECT endret_tidspunkt
                    FROM   oppgave_v1
                    WHERE  id = :id
                    """.trimIndent(),
                paramMap = mapOf("id" to oppgaveId),
            ).map { row ->
                row.sqlTimestamp("endret_tidspunkt")
            }.asSingle,
        )
    }!!
}
