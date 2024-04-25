package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Companion.fra
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class PostgresRepositoryTest {
    private val saksbehandlerIdent = "Z123456"
    private val testPerson = Person(ident = "12345678901")
    private val opprettetNå =
        ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Europe/Oslo")).truncatedTo(ChronoUnit.SECONDS)
    private val oppgaveIdTest = UUIDv7.ny()

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

            repo.lagre(lagOppgave(tilstand = FERDIG_BEHANDLET))
            repo.hentNesteOppgavenTil("NAVIdent2") shouldBe null
        }
    }

    @Test
    fun `Skal hente eldste oppgave som ikke er tatt av saksbehandler og er klar til behandling`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            val yngsteOppgave =
                lagOppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    opprettet = opprettetNå,
                )

            val eldsteOppgave =
                lagOppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    opprettet = opprettetNå.minusDays(10),
                )

            repo.lagre(yngsteOppgave)
            repo.lagre(eldsteOppgave)

            val saksbehandlerIdent = "NAVIdent"
            val nesteOppgave = repo.hentNesteOppgavenTil(saksbehandlerIdent)
            nesteOppgave!!.oppgaveId shouldBe eldsteOppgave.oppgaveId
            nesteOppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
            nesteOppgave.tilstand() shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Eldste ledige oppgave er neste oppgave for saksbehandler`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            val ledigOppgave =
                lagOppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    opprettet = opprettetNå,
                )

            val saksbehandlerIdent = "NAVIdent"
            val tildeltOppgave =
                lagOppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    opprettet = opprettetNå.minusDays(10),
                    saksbehandlerIdent = saksbehandlerIdent,
                )

            repo.lagre(ledigOppgave)
            repo.lagre(tildeltOppgave)

            val nesteOppgave = repo.hentNesteOppgavenTil(saksbehandlerIdent)
            nesteOppgave!!.oppgaveId shouldBe ledigOppgave.oppgaveId
            nesteOppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
            nesteOppgave.tilstand() shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Eldste oppgave som er klar til behandling er neste oppgave for saksbehandler`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            val ledigOppgave =
                lagOppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    opprettet = opprettetNå,
                )

            val tildeltBehandling =
                lagOppgave(
                    tilstand = UNDER_BEHANDLING,
                    opprettet = opprettetNå.minusDays(10),
                )

            repo.lagre(ledigOppgave)
            repo.lagre(tildeltBehandling)

            val saksbehandlerIdent = "NAVIdent"
            val nesteOppgave = repo.hentNesteOppgavenTil(saksbehandlerIdent)
            nesteOppgave!!.oppgaveId shouldBe ledigOppgave.oppgaveId
            nesteOppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
            nesteOppgave.tilstand() shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Skal kunne slette behandling`() {
        val testBehandling = lagBehandling()
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.hentBehandling(testBehandling.behandlingId) shouldNotBe null
            repo.slettBehandling(testBehandling.behandlingId)

            assertThrows<DataNotFoundException> {
                repo.hentBehandling(testBehandling.behandlingId)
            }

            assertThrows<DataNotFoundException> {
                repo.hentPerson(testPerson.ident)
            }

            assertThrows<DataNotFoundException> {
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
    fun `Skal kunne lagre en behandling`() {
        val testBehandling = lagBehandling()
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val behandlingFraDatabase = repo.hentBehandling(testBehandling.behandlingId)
            behandlingFraDatabase shouldBe testBehandling
        }
    }

    @Test
    fun `Skal kunne lagre en oppgave flere ganger`() {
        val testOppgave = lagOppgave()
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            shouldNotThrowAny {
                repo.lagre(testOppgave)
                repo.lagre(testOppgave)
            }
        }
    }

    @Test
    fun `Skal kunne lagre og hente en oppgave`() {
        val testOppgave = lagOppgave()
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testOppgave)
            val oppgaveFraDatabase = repo.hentOppgave(testOppgave.oppgaveId)
            oppgaveFraDatabase shouldBe testOppgave
        }
    }

    @Test
    fun `Skal kunne endre tilstand på en oppgave`() {
        val testOppgave = lagOppgave(tilstand = KLAR_TIL_BEHANDLING)
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            repo.lagre(testOppgave)
            repo.hentOppgave(testOppgave.oppgaveId).tilstand() shouldBe KLAR_TIL_BEHANDLING

            repo.lagre(testOppgave.copy(tilstand = Oppgave.FerdigBehandlet))
            repo.hentOppgave(testOppgave.oppgaveId).tilstand() shouldBe FERDIG_BEHANDLET
        }
    }

    @Test
    fun `Skal kunne hente oppgaver basert på tilstand`() {
        val oppgaveKlarTilBehandling = lagOppgave(tilstand = KLAR_TIL_BEHANDLING)
        val oppgaveFerdigBehandlet = lagOppgave(tilstand = FERDIG_BEHANDLET)

        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(oppgaveKlarTilBehandling)
            repo.lagre(oppgaveFerdigBehandlet)

            repo.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET).let { oppgaver ->
                oppgaver.size shouldBe 1
                oppgaver.single().oppgaveId shouldBe oppgaveFerdigBehandlet.oppgaveId
            }

            repo.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).let { oppgaver ->
                oppgaver.size shouldBe 1
                oppgaver.single().oppgaveId shouldBe oppgaveKlarTilBehandling.oppgaveId
            }
        }
    }

    @Test
    fun `Skal kunne hente alle oppgaver for en gitt person`() {
        val ola = Person(ident = "12345678910")
        val kari = Person(ident = "10987654321")

        val oppgave1TilOla = lagOppgave(person = ola, tilstand = KLAR_TIL_BEHANDLING)
        val oppgave2TilOla = lagOppgave(person = ola, tilstand = FERDIG_BEHANDLET)
        val oppgave1TilKari = lagOppgave(person = kari, tilstand = FERDIG_BEHANDLET)

        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(oppgave1TilOla)
            repo.lagre(oppgave2TilOla)
            repo.lagre(oppgave1TilKari)

            repo.finnOppgaverFor(ola.ident) shouldBe listOf(oppgave1TilOla, oppgave2TilOla)
            repo.finnOppgaverFor(kari.ident) shouldBe listOf(oppgave1TilKari)
        }
    }

    @Test
    fun `Skal hente oppgaveId fra behandlingId`() {
        val behandling = lagBehandling()
        val oppgave = lagOppgave(behandling = behandling)

        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(oppgave)
            repo.lagre(behandling)

            repo.hentOppgaveIdFor(behandlingId = behandling.behandlingId) shouldBe oppgave.oppgaveId
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
            val oppgave1 = lagOppgave(UNDER_BEHANDLING, enUkeSiden, saksbehandler1)
            val oppgave2 = lagOppgave(UNDER_BEHANDLING, saksbehandlerIdent = saksbehandler2)
            val oppgave3 = lagOppgave(FERDIG_BEHANDLET, saksbehandlerIdent = saksbehandler2)
            val oppgave4 = lagOppgave(UNDER_BEHANDLING, saksbehandlerIdent = null)

            repo.lagre(oppgave1)
            repo.lagre(oppgave2)
            repo.lagre(oppgave3)
            repo.lagre(oppgave4)

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
            val oppgave1 = lagOppgave(UNDER_BEHANDLING, enUkeSiden)
            val oppgave2 = lagOppgave(KLAR_TIL_BEHANDLING, saksbehandlerIdent = saksbehandlerIdent)
            val oppgave3 = lagOppgave(KLAR_TIL_BEHANDLING, saksbehandlerIdent = saksbehandlerIdent)
            val oppgave4 = lagOppgave(OPPRETTET, saksbehandlerIdent = saksbehandlerIdent)
            repo.lagre(oppgave1)
            repo.lagre(oppgave2)
            repo.lagre(oppgave3)
            repo.lagre(oppgave4)

            repo.søk(
                Søkefilter(
                    tilstand = setOf(UNDER_BEHANDLING),
                    periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                ),
            ).single() shouldBe oppgave1

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
                    periode =
                        Søkefilter.Periode(
                            fom = enUkeSiden.plusDays(1).toLocalDate(),
                            tom = enUkeSiden.plusDays(2).toLocalDate(),
                        ),
                ),
            ).size shouldBe 0

            repo.søk(
                Søkefilter(
                    tilstand = setOf(UNDER_BEHANDLING),
                    periode =
                        Søkefilter.Periode(
                            fom = enUkeSiden.minusDays(1).toLocalDate(),
                            tom = enUkeSiden.plusDays(2).toLocalDate(),
                        ),
                ),
            ).size shouldBe 1

            repo.søk(
                Søkefilter(
                    tilstand = setOf(KLAR_TIL_BEHANDLING),
                    periode =
                        Søkefilter.Periode(
                            fom = opprettetNå.toLocalDate(),
                            tom = opprettetNå.toLocalDate(),
                        ),
                ),
            ).size shouldBe 2
        }
    }

    @Test
    fun `Skal hente en oppgave basert på behandlingId`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            val oppgave = lagOppgave()
            repo.lagre(oppgave)
            repo.hentOppgaveFor(oppgave.behandlingId) shouldBe oppgave
        }
    }

    private fun lagOppgave(
        tilstand: Oppgave.Tilstand.Type = KLAR_TIL_BEHANDLING,
        opprettet: ZonedDateTime = opprettetNå,
        saksbehandlerIdent: String? = null,
        person: Person = testPerson,
        behandling: Behandling = lagBehandling(person = person),
    ): Oppgave {
        return Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            ident = person.ident,
            saksbehandlerIdent = saksbehandlerIdent,
            behandlingId = behandling.behandlingId,
            opprettet = opprettet,
            emneknagger = setOf(),
            tilstand = fra(tilstand),
            behandling = behandling,
        )
    }

    private fun lagBehandling(
        behandlingId: UUID = UUIDv7.ny(),
        opprettet: ZonedDateTime = opprettetNå,
        person: Person = testPerson,
    ): Behandling {
        return Behandling(
            behandlingId = behandlingId,
            person = person,
            opprettet = opprettet,
        )
    }
}
