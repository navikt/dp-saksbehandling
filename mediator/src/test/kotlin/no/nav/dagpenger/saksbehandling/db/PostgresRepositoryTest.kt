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
    private val saksbehandlerIdent = "Z123456"
    private val testPerson = Person(ident = "12345678901")
    private val opprettetNå =
        ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Europe/Oslo")).truncatedTo(ChronoUnit.SECONDS)
    private val testBehandling = lagBehandlingOgOppgaveMedTilstand(KLAR_TIL_BEHANDLING)
    private val behandlingIdTest = testBehandling.behandlingId
    private val oppgaveIdTest = testBehandling.oppgaver.first().oppgaveId

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
                    opprettet = opprettetNå,
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
                opprettet = opprettetNå,
            )

            val eldsteBehandling = lagBehandlingOgOppgaveMedTilstand(
                tilstand = KLAR_TIL_BEHANDLING,
                opprettet = opprettetNå.minusDays(10),
            )

            repo.lagre(yngsteBehandling)
            repo.lagre(eldsteBehandling)

            val saksbehandlerIdent = "NAVIdent"
            val nesteOppgave = repo.hentNesteOppgavenTil(saksbehandlerIdent)
            nesteOppgave!!.oppgaveId shouldBe eldsteBehandling.oppgaver.first().oppgaveId
            nesteOppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
            nesteOppgave.tilstand() shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Eldste ledige oppgave er neste oppgave for saksbehandler`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            val ledigBehandling = lagBehandlingOgOppgaveMedTilstand(
                tilstand = KLAR_TIL_BEHANDLING,
                opprettet = opprettetNå,
            )

            val saksbehandlerIdent = "NAVIdent"
            val tildeltBehandling = lagBehandlingOgOppgaveMedTilstand(
                tilstand = KLAR_TIL_BEHANDLING,
                opprettet = opprettetNå.minusDays(10),
                saksbehandlerIdent = saksbehandlerIdent,
            )

            repo.lagre(ledigBehandling)
            repo.lagre(tildeltBehandling)

            val nesteOppgave = repo.hentNesteOppgavenTil(saksbehandlerIdent)
            nesteOppgave!!.oppgaveId shouldBe ledigBehandling.oppgaver.first().oppgaveId
            nesteOppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
            nesteOppgave.tilstand() shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Eldste oppgave som er klar til behandling er neste oppgave for saksbehandler`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            val ledigBehandling = lagBehandlingOgOppgaveMedTilstand(
                tilstand = KLAR_TIL_BEHANDLING,
                opprettet = opprettetNå,
            )

            val tildeltBehandling = lagBehandlingOgOppgaveMedTilstand(
                tilstand = UNDER_BEHANDLING,
                opprettet = opprettetNå.minusDays(10),
            )

            repo.lagre(ledigBehandling)
            repo.lagre(tildeltBehandling)

            val saksbehandlerIdent = "NAVIdent"
            val nesteOppgave = repo.hentNesteOppgavenTil(saksbehandlerIdent)
            nesteOppgave!!.oppgaveId shouldBe ledigBehandling.oppgaver.first().oppgaveId
            nesteOppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
            nesteOppgave.tilstand() shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
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
                repo.hentOppgave(oppgaveIdTest)
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
                repo.hentBehandling(UUIDv7.ny())
            }
        }
    }

    @Test
    fun `Skal kunne lagre en behandling med oppgave`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val behandlingFraDatabase = repo.hentBehandling(behandlingIdTest)
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
        // oppgaveKlarTilBehandling.tildel(OppgaveAnsvarHendelse(oppgaveId, navIdent))
        testBehandling.oppgaver.first().tildel(OppgaveAnsvarHendelse(oppgaveIdTest, navIdent))
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val oppgaveFraDatabase = repo.hentOppgave(oppgaveIdTest)
            oppgaveFraDatabase.saksbehandlerIdent shouldBe navIdent
            oppgaveFraDatabase.tilstand() shouldBe UNDER_BEHANDLING
        }
    }

    @Test
    fun `Skal kunne lagre og hente en oppgave`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val oppgaveFraDatabase = repo.hentOppgave(oppgaveIdTest)
            oppgaveFraDatabase shouldBe testBehandling.oppgaver.first()
        }
    }

    @Test
    fun `Skal kunne endre tilstand på en oppgave`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            repo.lagre(testBehandling)
            repo.hentOppgave(oppgaveIdTest).tilstand() shouldBe KLAR_TIL_BEHANDLING

            repo.lagre(
                testBehandling.copy(
                    oppgaver = mutableListOf(
                        testBehandling.oppgaver.first().copy(tilstand = Oppgave.FerdigBehandlet),
                    ),
                ),
            )
            repo.hentOppgave(oppgaveIdTest).tilstand() shouldBe FERDIG_BEHANDLET
        }
    }

    @Test
    fun `Skal kunne lagre en behandling og hente den igjen på bakgrunn av en oppgaveId`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val behandlingFraDatabase = repo.hentBehandlingFra(oppgaveIdTest)
            behandlingFraDatabase shouldBe testBehandling
        }
    }

    @Test
    fun `Skal kunne hente oppgaver basert på tilstand`() {
        val behandlingId2 = UUIDv7.ny()
        val oppgaveId2 = UUIDv7.ny()
        val oppgave2 = Oppgave.rehydrer(
            oppgaveId = oppgaveId2,
            ident = testPerson.ident,
            saksbehandlerIdent = null,
            behandlingId = behandlingId2,
            opprettet = opprettetNå,
            emneknagger = setOf("Søknadsbehandling, Utland"),
            tilstand = Oppgave.KlarTilBehandling,
        )

        val oppgaveId3 = UUIDv7.ny()
        val testBehandling2 = Behandling(
            behandlingId = behandlingId2,
            person = testPerson,
            opprettet = opprettetNå,
            oppgaver = mutableListOf(
                oppgave2,
                oppgave2.copy(oppgaveId = oppgaveId3, tilstand = Oppgave.FerdigBehandlet),
            ),
        )

        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.lagre(testBehandling2)

            val ferdigBehandledeOppgaver = repo.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET)
            ferdigBehandledeOppgaver.size shouldBe 1
            ferdigBehandledeOppgaver.single().oppgaveId shouldBe oppgaveId3

            val oppgaverTilBehandling = repo.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
            oppgaverTilBehandling.size shouldBe 2
            oppgaverTilBehandling.map { it.oppgaveId } shouldBe listOf(oppgaveIdTest, oppgaveId2)
        }
    }

    @Test
    fun `Skal kunne hente alle oppgaver for en gitt person`() {
        val oppgaveId3 = UUIDv7.ny()
        val testPerson2 = Person(ident = "02020288888")
        val behandlingId2 = UUIDv7.ny()
        val oppgaveId2 = UUIDv7.ny()
        val oppgave2 = Oppgave(
            oppgaveId = oppgaveId2,
            ident = testPerson2.ident,
            emneknagger = setOf("Søknadsbehandling, Utland"),
            opprettet = opprettetNå,
            behandlingId = behandlingId2,
            tilstand = Oppgave.KlarTilBehandling,
        )
        val testBehandling2 = Behandling(
            behandlingId = behandlingId2,
            person = testPerson2,
            opprettet = opprettetNå,
            oppgaver = mutableListOf(
                oppgave2,
                oppgave2.copy(oppgaveId = oppgaveId3, tilstand = Oppgave.FerdigBehandlet),
            ),
        )

        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.lagre(testBehandling2)

            val oppgaverTilPerson1 = repo.finnOppgaverFor(testPerson.ident)
            oppgaverTilPerson1.size shouldBe 1
            oppgaverTilPerson1.single().oppgaveId shouldBe oppgaveIdTest

            val oppgaverTilPerson2 = repo.finnOppgaverFor(testPerson2.ident)
            oppgaverTilPerson2.size shouldBe 2
            oppgaverTilPerson2.map { it.oppgaveId } shouldBe listOf(oppgaveId2, oppgaveId3)
        }
    }

    @Test
    fun `Skal hente oppgaveId fra behandlingId`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.hentOppgaveIdFor(behandlingId = testBehandling.behandlingId) shouldBe oppgaveIdTest
            repo.hentOppgaveIdFor(behandlingId = UUIDv7.ny()) shouldBe null
        }
    }

    @Test
    fun `Skal kunne søke etter mine oppgaver`() {
        val enUkeSiden = opprettetNå.minusDays(7)
        val saksbehandler1 = "saksbehandler1"
        val saksbehandler2 = "saksbehandler2"
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            val behandling1 = lagBehandlingOgOppgaveMedTilstand(UNDER_BEHANDLING, enUkeSiden, saksbehandler1)
            val behandling2 =
                lagBehandlingOgOppgaveMedTilstand(UNDER_BEHANDLING, saksbehandlerIdent = saksbehandler2)
            val behandling3 =
                lagBehandlingOgOppgaveMedTilstand(FERDIG_BEHANDLET, saksbehandlerIdent = saksbehandler2)
            val behandling4 = lagBehandlingOgOppgaveMedTilstand(UNDER_BEHANDLING, saksbehandlerIdent = null)
            repo.lagre(behandling1)
            repo.lagre(behandling2)
            repo.lagre(behandling3)
            repo.lagre(behandling4)

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                    saksbehandlerIdent = saksbehandler2,
                ),
            ).size shouldBe 2

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                    saksbehandlerIdent = saksbehandler1,
                ),
            ).size shouldBe 1

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                    saksbehandlerIdent = null,
                ),
            ).size shouldBe 4
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver`() {
        val enUkeSiden = opprettetNå.minusDays(7)

        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            val behandling1 = lagBehandlingOgOppgaveMedTilstand(UNDER_BEHANDLING, enUkeSiden)
            val behandling2 =
                lagBehandlingOgOppgaveMedTilstand(KLAR_TIL_BEHANDLING, saksbehandlerIdent = saksbehandlerIdent)
            val behandling3 =
                lagBehandlingOgOppgaveMedTilstand(KLAR_TIL_BEHANDLING, saksbehandlerIdent = saksbehandlerIdent)
            val behandling4 = lagBehandlingOgOppgaveMedTilstand(OPPRETTET, saksbehandlerIdent = saksbehandlerIdent)
            repo.lagre(behandling1)
            repo.lagre(behandling2)
            repo.lagre(behandling3)
            repo.lagre(behandling4)

            repo.søk(
                Søkefilter(
                    tilstand = setOf(UNDER_BEHANDLING),
                    periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                ),
            ).single().oppgaveId shouldBe behandling1.oppgaver.single().oppgaveId

            repo.søk(
                Søkefilter(
                    tilstand = setOf(KLAR_TIL_BEHANDLING, UNDER_BEHANDLING),
                    periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                ),
            ).size shouldBe 3

            repo.søk(Søkefilter.DEFAULT_SØKEFILTER).let {
                it.size shouldBe 2
                it.all { oppgave -> oppgave.tilstand() == KLAR_TIL_BEHANDLING } shouldBe true
            }

            repo.søk(
                Søkefilter(
                    tilstand = setOf(KLAR_TIL_BEHANDLING),
                    periode = Søkefilter.Periode(
                        fom = enUkeSiden.plusDays(1).toLocalDate(),
                        tom = enUkeSiden.plusDays(2).toLocalDate(),
                    ),
                ),
            ).size shouldBe 0

            repo.søk(
                Søkefilter(
                    tilstand = setOf(UNDER_BEHANDLING),
                    periode = Søkefilter.Periode(
                        fom = enUkeSiden.minusDays(1).toLocalDate(),
                        tom = enUkeSiden.plusDays(2).toLocalDate(),
                    ),
                ),
            ).size shouldBe 1

            repo.søk(
                Søkefilter(
                    tilstand = setOf(KLAR_TIL_BEHANDLING),
                    periode = Søkefilter.Periode(
                        fom = opprettetNå.toLocalDate(),
                        tom = opprettetNå.toLocalDate(),
                    ),
                ),
            ).size shouldBe 2
        }
    }

    private fun lagBehandlingOgOppgaveMedTilstand(
        tilstand: Oppgave.Tilstand.Type,
        opprettet: ZonedDateTime = opprettetNå,
        saksbehandlerIdent: String? = null,
    ): Behandling {
        val behandlingId = UUIDv7.ny()
        val oppgave = Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            ident = testPerson.ident,
            saksbehandlerIdent = saksbehandlerIdent,
            behandlingId = behandlingId,
            opprettet = opprettet,
            emneknagger = setOf("Søknadsbehandling"),
            tilstand = when (tilstand) {
                OPPRETTET -> Oppgave.Opprettet
                KLAR_TIL_BEHANDLING -> Oppgave.KlarTilBehandling
                UNDER_BEHANDLING -> Oppgave.UnderBehandling
                FERDIG_BEHANDLET -> Oppgave.FerdigBehandlet
            },
        )
        return Behandling(
            behandlingId = behandlingId,
            person = testPerson,
            opprettet = opprettet,
            oppgaver = mutableListOf(oppgave),
        )
    }
}
