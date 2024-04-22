package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class PostgresRepositoryTest {
    private val testPerson = Person(ident = "12345678901")
    private val behandlingId1 = UUIDv7.ny()
    private val oppgaveId = UUIDv7.ny()
    private val saksbehandlerIdent1 = "saksbehandler1"
    private val saksbehandlerIdent2 = "saksbehandler2"
    private val opprettetTidspunkt =
        ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Europe/Oslo")).truncatedTo(ChronoUnit.SECONDS)

    private val oppgaveKlarTilBehandling = Oppgave(
        oppgaveId = oppgaveId,
        ident = testPerson.ident,
        emneknagger = setOf("Søknadsbehandling"),
        opprettet = opprettetTidspunkt,
        behandlingId = behandlingId1,
        tilstand = KLAR_TIL_BEHANDLING,

    )
    private val testBehandling = Behandling(
        behandlingId = behandlingId1,
        person = testPerson,
        opprettet = opprettetTidspunkt,
        oppgaver = mutableListOf(oppgaveKlarTilBehandling),
    )

    private val testPerson2 = Person(ident = "12345678902")
    private val behandlingId2 = UUIDv7.ny()
    private val oppgaveId2 = UUIDv7.ny()
    private val oppgaveId3 = UUIDv7.ny()
    private val oppgave2 = Oppgave(
        oppgaveId = oppgaveId2,
        ident = testPerson2.ident,
        emneknagger = setOf("Søknadsbehandling, Utland"),
        opprettet = opprettetTidspunkt,
        behandlingId = behandlingId2,
        tilstand = KLAR_TIL_BEHANDLING,
    )

    private val testBehandling2 = Behandling(
        behandlingId = behandlingId2,
        person = testPerson2,
        opprettet = opprettetTidspunkt,
        oppgaver = mutableListOf(oppgave2, oppgave2.copy(oppgaveId = oppgaveId3, tilstand = FERDIG_BEHANDLET)),
    )

    @Test
    fun `Skal kunne lagre og hente person`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testPerson)

            val personFraDatabase = repo.finnPerson(testPerson.ident)
            personFraDatabase shouldBe testPerson
        }
    }

    @Test
    fun `Det finnes ikke flere ledige oppgaver`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            repo.lagre(
                lagBehandlingOgOppgaveMedTilstand(
                    tilstand = KLAR_TIL_BEHANDLING,
                    saksbehandlerIdent = "NAVIdent",
                    opprettet = opprettetTidspunkt,
                ),
            )
            repo.hentNesteOppgavenTil("NAVIdent2") shouldBe null
        }
    }

    @Test
    fun `Skal hente eldste oppgave som ikke er tatt av saksbehandler og er klar til behandling`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            val yngsteBehandling = lagBehandlingOgOppgaveMedTilstand(
                tilstand = KLAR_TIL_BEHANDLING,
                saksbehandlerIdent = null,
                opprettet = opprettetTidspunkt,
            )

            val eldsteBehandling = lagBehandlingOgOppgaveMedTilstand(
                tilstand = KLAR_TIL_BEHANDLING,
                saksbehandlerIdent = null,
                opprettet = opprettetTidspunkt.minusDays(10),
            )

            repo.lagre(yngsteBehandling)
            repo.lagre(eldsteBehandling)

            val saksbehandlerIdent = "NAVIdent"
            val nesteOppgave = repo.hentNesteOppgavenTil(saksbehandlerIdent)
            nesteOppgave!!.oppgaveId shouldBe eldsteBehandling.oppgaver.first().oppgaveId
            nesteOppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
            nesteOppgave.tilstand shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Eldste ledige oppgave er neste oppgave for saksbehandler`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            val ledigBehandling = lagBehandlingOgOppgaveMedTilstand(
                tilstand = KLAR_TIL_BEHANDLING,
                saksbehandlerIdent = null,
                opprettet = opprettetTidspunkt,
            )

            val saksbehandlerIdent = "NAVIdent"
            val tildeltBehandling = lagBehandlingOgOppgaveMedTilstand(
                tilstand = KLAR_TIL_BEHANDLING,
                saksbehandlerIdent = saksbehandlerIdent,
                opprettet = opprettetTidspunkt.minusDays(10),
            )

            repo.lagre(ledigBehandling)
            repo.lagre(tildeltBehandling)

            val nesteOppgave = repo.hentNesteOppgavenTil(saksbehandlerIdent)
            nesteOppgave!!.oppgaveId shouldBe ledigBehandling.oppgaver.first().oppgaveId
            nesteOppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
            nesteOppgave.tilstand shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Eldste oppgave som er klar til behandling er neste oppgave for saksbehandler`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            val ledigBehandling = lagBehandlingOgOppgaveMedTilstand(
                tilstand = KLAR_TIL_BEHANDLING,
                saksbehandlerIdent = null,
                opprettet = opprettetTidspunkt,
            )

            val tildeltBehandling = lagBehandlingOgOppgaveMedTilstand(
                tilstand = UNDER_BEHANDLING,
                saksbehandlerIdent = null,
                opprettet = opprettetTidspunkt.minusDays(10),
            )

            repo.lagre(ledigBehandling)
            repo.lagre(tildeltBehandling)

            val saksbehandlerIdent = "NAVIdent"
            val nesteOppgave = repo.hentNesteOppgavenTil(saksbehandlerIdent)
            nesteOppgave!!.oppgaveId shouldBe ledigBehandling.oppgaver.first().oppgaveId
            nesteOppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
            nesteOppgave.tilstand shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Skal kunne slette behandling`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.hentBehandling(testBehandling.behandlingId) shouldNotBe null
            repo.slettBehandling(testBehandling.behandlingId)

            assertThrows<DataNotFoundException> {
                repo.hentBehandling(testBehandling.behandlingId)
                repo.hentPerson(testPerson.ident)
                repo.hentOppgave(oppgaveId)
            }
        }
    }

    @Test
    fun `Exception hvis vi ikke får hentet person basert på ident`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            shouldThrow<DataNotFoundException> {
                repo.hentPerson(testPerson.ident)
            }
        }
    }

    @Test
    fun `Exception hvis vi ikke får hentet behandling basert på behandlingId`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            shouldThrow<DataNotFoundException> {
                repo.hentBehandling(behandlingId1)
            }
        }
    }

    @Test
    fun `Skal kunne lagre en behandling med oppgave`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val behandlingFraDatabase = repo.hentBehandling(behandlingId1)
            behandlingFraDatabase shouldBe testBehandling
        }
    }

    @Test
    fun `Skal kunne lagre en behandling med oppgave flere ganger`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            shouldNotThrowAny {
                repo.lagre(testBehandling)
                repo.lagre(testBehandling)
            }
        }
    }

    @Test
    fun `Skal kunne lagre og hente endring av ansvarlig saksbehandler`() {
        val navIdent = "Z999999"
        oppgaveKlarTilBehandling.tildel(OppgaveAnsvarHendelse(oppgaveId, navIdent))
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val oppgaveFraDatabase = repo.hentOppgave(oppgaveId)
            oppgaveFraDatabase.saksbehandlerIdent shouldBe navIdent
            oppgaveFraDatabase.tilstand shouldBe UNDER_BEHANDLING
        }
    }

    @Test
    fun `Skal kunne lagre og hente en oppgave`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val oppgaveFraDatabase = repo.hentOppgave(oppgaveId)
            oppgaveFraDatabase shouldBe oppgaveKlarTilBehandling
        }
    }

    @Test
    fun `Skal kunne endre tilstand på en oppgave`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.hentOppgave(oppgaveId).tilstand shouldBe KLAR_TIL_BEHANDLING

            repo.lagre(testBehandling.copy(oppgaver = mutableListOf(oppgaveKlarTilBehandling.copy(tilstand = FERDIG_BEHANDLET))))
            repo.hentOppgave(oppgaveId).tilstand shouldBe FERDIG_BEHANDLET
        }
    }

    @Test
    fun `Skal kunne lagre en behandling og hente den igjen på bakgrunn av en oppgaveId`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val behandlingFraDatabase = repo.hentBehandlingFra(oppgaveId)
            behandlingFraDatabase shouldBe testBehandling
        }
    }

    @Test
    fun `Skal kunne hente oppgaver basert på tilstand`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.lagre(testBehandling2)

            val ferdigBehandledeOppgaver = repo.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET)
            ferdigBehandledeOppgaver.size shouldBe 1
            ferdigBehandledeOppgaver.single().oppgaveId shouldBe oppgaveId3

            val oppgaverTilBehandling = repo.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
            oppgaverTilBehandling.size shouldBe 2
            oppgaverTilBehandling.map { it.oppgaveId } shouldBe listOf(oppgaveId, oppgaveId2)
        }
    }

    @Test
    fun `Skal kunne hente alle oppgaver for en gitt person`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.lagre(testBehandling2)

            val oppgaverTilPerson1 = repo.finnOppgaverFor(testPerson.ident)
            oppgaverTilPerson1.size shouldBe 1
            oppgaverTilPerson1.single().oppgaveId shouldBe oppgaveId

            val oppgaverTilPerson2 = repo.finnOppgaverFor(testPerson2.ident)
            oppgaverTilPerson2.size shouldBe 2
            oppgaverTilPerson2.map { it.oppgaveId } shouldBe listOf(oppgaveId2, oppgaveId3)
        }
    }

    @Test
    fun `Skal kunne hente alle oppgaver for saksbehandler`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            val behandling1 = lagBehandlingOgOppgaveMedTilstand(UNDER_BEHANDLING, saksbehandlerIdent1)
            val behandling2 = lagBehandlingOgOppgaveMedTilstand(UNDER_BEHANDLING, saksbehandlerIdent1)
            val behandling3 = lagBehandlingOgOppgaveMedTilstand(FERDIG_BEHANDLET, saksbehandlerIdent1)
            val behandling4 = lagBehandlingOgOppgaveMedTilstand(UNDER_BEHANDLING, saksbehandlerIdent2)
            repo.lagre(behandling1)
            repo.lagre(behandling2)
            repo.lagre(behandling3)
            repo.lagre(behandling4)

            repo.finnSaksbehandlersOppgaver(saksbehandlerIdent1).size shouldBe 3
            repo.finnSaksbehandlersOppgaver(saksbehandlerIdent2).size shouldBe 1
        }
    }

    @Test
    fun `Skal hente oppgaveId fra behandlingId`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.hentOppgaveIdFor(behandlingId = testBehandling.behandlingId) shouldBe testBehandling.oppgaver.first().oppgaveId
            repo.hentOppgaveIdFor(behandlingId = UUIDv7.ny()) shouldBe null
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver`() {
        val opprettet = ZonedDateTime.parse("2024-01-01T00:00:00Z")

        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            val behandling1 = lagBehandlingOgOppgaveMedTilstand(
                tilstand = UNDER_BEHANDLING,
                opprettet = opprettet,
                saksbehandlerIdent = saksbehandlerIdent1,
            )
            val behandling2 = lagBehandlingOgOppgaveMedTilstand(KLAR_TIL_BEHANDLING, saksbehandlerIdent1)
            val behandling3 = lagBehandlingOgOppgaveMedTilstand(KLAR_TIL_BEHANDLING, saksbehandlerIdent1)
            val behandling4 = lagBehandlingOgOppgaveMedTilstand(OPPRETTET, saksbehandlerIdent1)

            repo.lagre(behandling1)
            repo.lagre(behandling2)
            repo.lagre(behandling3)
            repo.lagre(behandling4)

            repo.søk(Søkefilter(tilstand = UNDER_BEHANDLING, periode = Søkefilter.Periode.TOM_PERIODE))
                .single().oppgaveId shouldBe behandling1.oppgaver.single().oppgaveId

            repo.søk(Søkefilter.DEFAULT_SØKEFILTER).let {
                it.size shouldBe 2
                it.all { oppgave -> oppgave.tilstand == KLAR_TIL_BEHANDLING } shouldBe true
            }

            repo.søk(
                Søkefilter(
                    tilstand = KLAR_TIL_BEHANDLING,
                    periode = Søkefilter.Periode(
                        fom = opprettet.plusDays(1).toLocalDate(),
                        tom = opprettet.plusDays(2).toLocalDate(),
                    ),
                ),
            ).size shouldBe 0

            repo.søk(
                Søkefilter(
                    tilstand = UNDER_BEHANDLING,
                    periode = Søkefilter.Periode(
                        fom = opprettet.minusDays(1).toLocalDate(),
                        tom = opprettet.plusDays(2).toLocalDate(),
                    ),
                ),
            ).size shouldBe 1
        }
    }

    private fun lagBehandlingOgOppgaveMedTilstand(
        tilstand: Oppgave.Tilstand.Type,
        saksbehandlerIdent: String?,
        opprettet: ZonedDateTime = opprettetTidspunkt,
    ): Behandling {
        val behandlingId = UUIDv7.ny()
        val oppgave = Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            ident = testPerson.ident,
            emneknagger = setOf("Søknadsbehandling"),
            opprettet = opprettet,
            behandlingId = behandlingId,
            tilstand = tilstand,
            saksbehandlerIdent = saksbehandlerIdent,
        )
        return Behandling(
            behandlingId = behandlingId,
            person = testPerson,
            opprettet = opprettet,
            oppgaver = mutableListOf(oppgave),
        )
    }
}
