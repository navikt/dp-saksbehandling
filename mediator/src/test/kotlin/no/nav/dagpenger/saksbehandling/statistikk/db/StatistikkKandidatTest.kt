package no.nav.dagpenger.saksbehandling.statistikk.db

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.testPerson
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

/**
 * Kandidattabellen er det som erstatter kursoren i statistikkeksporten. Disse testene fester
 * invariantene den hviler på: at hver lagret tilstandsendring blir kandidat, og at det ikke blir
 * kandidat av noe som ikke ble lagret.
 */
class StatistikkKandidatTest {
    private fun DataSource.kandidater(): List<UUID> =
        sessionOf(this).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = "SELECT tilstand_id FROM statistikk_kandidat_v1 WHERE vurdert IS NULL",
                ).map { it.uuid("tilstand_id") }.asList,
            )
        }

    @Test
    fun `Hver lagret tilstandsendring blir kandidat`() {
        val behandling = TestHelper.lagBehandling()
        val oppgave =
            TestHelper.lagOppgave(
                behandling = behandling,
                tilstand = Oppgave.KlarTilBehandling,
                tilstandslogg =
                    OppgaveTilstandslogg().also {
                        it.leggTil(nyTilstand = KLAR_TIL_BEHANDLING, hendelse = TomHendelse)
                    },
            )

        DBTestHelper.withMigratedDb { ds ->
            this.opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                behandling = behandling,
                sak = Sak(opprettet = LocalDateTime.now()),
                oppgave = oppgave,
                merkSomEgenSak = true,
            )

            ds.kandidater() shouldBe oppgave.tilstandslogg.map { it.id }

            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = TestHelper.saksbehandler.navIdent,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )
            PostgresOppgaveRepository(DatabaseSession(ds)).lagre(oppgave)

            ds.kandidater().toSet() shouldBe oppgave.tilstandslogg.map { it.id }.toSet()
        }
    }

    @Test
    fun `Lagring av samme tilstandsendring flere ganger gir ikke duplikate kandidater`() {
        val behandling = TestHelper.lagBehandling()
        val oppgave =
            TestHelper.lagOppgave(
                behandling = behandling,
                tilstand = Oppgave.KlarTilBehandling,
                tilstandslogg =
                    OppgaveTilstandslogg().also {
                        it.leggTil(nyTilstand = KLAR_TIL_BEHANDLING, hendelse = TomHendelse)
                    },
            )

        DBTestHelper.withMigratedDb { ds ->
            this.opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                behandling = behandling,
                sak = Sak(opprettet = LocalDateTime.now()),
                oppgave = oppgave,
                merkSomEgenSak = true,
            )
            val repository = PostgresOppgaveRepository(DatabaseSession(ds))
            repository.lagre(oppgave)
            repository.lagre(oppgave)

            ds.kandidater() shouldBe oppgave.tilstandslogg.map { it.id }
        }
    }

    /**
     * V115 har en unik partiell indeks på (oppgave_id, tilstand) for avsluttende tilstander. Blir
     * logg-inserten no-op på grunn av den, skjedde det ingen tilstandsendring, og da skal det
     * heller ikke bli kandidat. Dette er invarianten `antallLagrede > 0` i
     * PostgresOppgaveRepository beskytter.
     */
    @Test
    fun `Tilstandsendring som avvises av unik indeks blir ikke kandidat`() {
        val behandling = TestHelper.lagBehandling()
        val tilstandslogg =
            OppgaveTilstandslogg().also {
                it.leggTil(nyTilstand = KLAR_TIL_BEHANDLING, hendelse = TomHendelse)
                it.leggTil(nyTilstand = FERDIG_BEHANDLET, hendelse = TomHendelse)
                it.leggTil(nyTilstand = FERDIG_BEHANDLET, hendelse = TomHendelse)
            }
        val oppgave =
            TestHelper.lagOppgave(
                behandling = behandling,
                tilstand = Oppgave.FerdigBehandlet,
                tilstandslogg = tilstandslogg,
            )

        DBTestHelper.withMigratedDb { ds ->
            this.opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                behandling = behandling,
                sak = Sak(opprettet = LocalDateTime.now()),
                oppgave = oppgave,
                merkSomEgenSak = true,
            )

            val loggIder =
                sessionOf(ds).use { session ->
                    session.run(
                        queryOf(
                            //language=PostgreSQL
                            statement = "SELECT id FROM oppgave_tilstand_logg_v1 WHERE oppgave_id = :id",
                            paramMap = mapOf("id" to oppgave.oppgaveId),
                        ).map { it.uuid("id") }.asList,
                    )
                }

            loggIder.size shouldBe 2
            ds.kandidater().toSet() shouldBe loggIder.toSet()
        }
    }
}
