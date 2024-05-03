package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
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
import java.time.LocalDate
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
            repo.tildelNesteOppgaveTil("NAVIdent2") shouldBe null
        }
    }

    @Test
    fun `Ved tildeling av neste oppgave, skal man finne eldste ledige oppgave klar til behandling og oppdatere den`() {
        withMigratedDb { ds ->
            val testSaksbehandler = "NAVIdent"
            val repo = PostgresRepository(ds)

            val yngsteLedigeOppgave =
                lagOppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    opprettet = opprettetNå,
                )

            val eldsteLedigeOppgave =
                lagOppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    opprettet = opprettetNå.minusDays(10),
                )

            val endaEldreTildeltOppgave =
                lagOppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    opprettet = opprettetNå.minusDays(11),
                    saksbehandlerIdent = saksbehandlerIdent,
                )

            val endaEldreFerdigOppgave =
                lagOppgave(
                    tilstand = FERDIG_BEHANDLET,
                    opprettet = opprettetNå.minusDays(12),
                    saksbehandlerIdent = testSaksbehandler,
                )

            val endaEldreOpprettetOppgave =
                lagOppgave(
                    tilstand = OPPRETTET,
                    opprettet = opprettetNå.minusDays(13),
                )

            repo.lagre(yngsteLedigeOppgave)
            repo.lagre(eldsteLedigeOppgave)
            repo.lagre(endaEldreTildeltOppgave)
            repo.lagre(endaEldreFerdigOppgave)
            repo.lagre(endaEldreOpprettetOppgave)

            val nesteOppgave = repo.tildelNesteOppgaveTil(testSaksbehandler)
            nesteOppgave!!.oppgaveId shouldBe eldsteLedigeOppgave.oppgaveId
            nesteOppgave.saksbehandlerIdent shouldBe testSaksbehandler
            nesteOppgave.tilstand() shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Skal kunne slette behandling`() {
        val testOppgave = lagOppgave(emneknagger = setOf("hugga", "bugga"))
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testOppgave)
            repo.slettBehandling(testOppgave.behandlingId)

            assertThrows<DataNotFoundException> {
                repo.hentBehandling(testOppgave.behandlingId)
            }

            assertThrows<DataNotFoundException> {
                repo.hentPerson(testPerson.ident)
            }

            assertThrows<DataNotFoundException> {
                repo.hentOppgave(oppgaveIdTest)
            }

            sessionOf(ds).use { session ->
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement = """SELECT COUNT(*) FROM emneknagg_v1 WHERE oppgave_id = '${testOppgave.oppgaveId}'""",
                    ).map { row ->
                        row.int(1)
                    }.asSingle,
                )
            } shouldBe 0
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
    fun `Skal kunne søke etter oppgaver filtrert på tilstand`() {
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
    fun `Skal kunne søke etter oppgaver filtrert på emneknagger`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            val oppgave1 = lagOppgave(emneknagger = setOf("hubba", "bubba"))
            val oppgave2 = lagOppgave(emneknagger = setOf("hubba"))
            val oppgave3 = lagOppgave(emneknagger = emptySet())

            repo.lagre(oppgave1)
            repo.lagre(oppgave2)
            repo.lagre(oppgave3)

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                    emneknagg = emptySet(),
                ),
            ) shouldBe listOf(oppgave1, oppgave2, oppgave3)

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                    emneknagg = setOf("hubba"),
                ),
            ) shouldBe listOf(oppgave1, oppgave2)

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                    emneknagg = setOf("bubba"),
                ),
            ) shouldBe listOf(oppgave1)

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                    emneknagg = setOf("bubba", "hubba"),
                ),
            ) shouldBe listOf(oppgave1, oppgave2)
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver tildel en gitt saksbehandler`() {
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
                    saksbehandlerIdent = saksbehandler1,
                ),
            ).size shouldBe 1

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
                    saksbehandlerIdent = null,
                ),
            ).size shouldBe 4
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver filtrert på tilstand og opprettet`() {
        val enUkeSiden = opprettetNå.minusDays(7)

        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            val oppgaveUnderBehandlingEnUkeGammel =
                lagOppgave(UNDER_BEHANDLING, opprettet = enUkeSiden, saksbehandlerIdent = saksbehandlerIdent)
            val oppgaveKlarTilBehandlingIDag = lagOppgave(KLAR_TIL_BEHANDLING)
            val oppgaveKlarTilBehandlingIGår = lagOppgave(KLAR_TIL_BEHANDLING, opprettet = opprettetNå.minusDays(1))
            val oppgaveOpprettetIDag = lagOppgave(OPPRETTET)
            repo.lagre(oppgaveUnderBehandlingEnUkeGammel)
            repo.lagre(oppgaveKlarTilBehandlingIDag)
            repo.lagre(oppgaveKlarTilBehandlingIGår)
            repo.lagre(oppgaveOpprettetIDag)

            repo.søk(
                Søkefilter(
                    tilstand = setOf(UNDER_BEHANDLING),
                    periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                ),
            ).single() shouldBe oppgaveUnderBehandlingEnUkeGammel

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
            ).size shouldBe 1
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver opprettet en bestemt dato, uavhengig av tid på døgnet`() {
        withMigratedDb { ds ->
            val iDag = LocalDate.now()
            val iGår = iDag.minusDays(1)
            val iForgårsSåSeintPåDagenSomMulig: ZonedDateTime =
                ZonedDateTime.of(
                    iGår.atStartOfDay().minusNanos(1),
                    ZoneId.of("Europe/Oslo"),
                )
            val iGårSåTidligPåDagenSomMulig: ZonedDateTime = ZonedDateTime.of(iGår.atStartOfDay(), ZoneId.of("Europe/Oslo"))
            val iGårSåSeintPåDagenSomMulig: ZonedDateTime = ZonedDateTime.of(iDag.atStartOfDay().minusNanos(1), ZoneId.of("Europe/Oslo"))
            val iDagSåTidligPåDagenSomMulig: ZonedDateTime = ZonedDateTime.of(iDag.atStartOfDay(), ZoneId.of("Europe/Oslo"))
            val repo = PostgresRepository(ds)
            val oppgaveOpprettetSeintForgårs = lagOppgave(KLAR_TIL_BEHANDLING, opprettet = iForgårsSåSeintPåDagenSomMulig)
            val oppgaveOpprettetTidligIGår = lagOppgave(KLAR_TIL_BEHANDLING, opprettet = iGårSåTidligPåDagenSomMulig)
            val oppgaveOpprettetSeintIGår = lagOppgave(KLAR_TIL_BEHANDLING, opprettet = iGårSåSeintPåDagenSomMulig)
            val oppgaveOpprettetTidligIDag = lagOppgave(KLAR_TIL_BEHANDLING, opprettet = iDagSåTidligPåDagenSomMulig)

            repo.lagre(oppgaveOpprettetSeintForgårs)
            repo.lagre(oppgaveOpprettetTidligIGår)
            repo.lagre(oppgaveOpprettetSeintIGår)
            repo.lagre(oppgaveOpprettetTidligIDag)

            val oppgaver =
                repo.søk(
                    Søkefilter(
                        tilstand = setOf(KLAR_TIL_BEHANDLING),
                        periode = Søkefilter.Periode(fom = iGår, tom = iGår),
                    ),
                )
            oppgaver.size shouldBe 2
            oppgaver.contains(oppgaveOpprettetTidligIGår)
            oppgaver.contains(oppgaveOpprettetSeintIGår)
        }
    }

    @Test
    fun `Skal hente en oppgave basert på behandlingId`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            val oppgave = lagOppgave()
            repo.lagre(oppgave)
            repo.hentOppgaveFor(oppgave.behandlingId) shouldBe oppgave

            assertThrows<DataNotFoundException> {
                repo.hentOppgaveFor(behandlingId = UUIDv7.ny())
            }
        }
    }

    @Test
    fun `Skal finne en oppgave basert på behandlingId hvis den finnes`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            val oppgave = lagOppgave()
            repo.lagre(oppgave)
            repo.finnOppgaveFor(oppgave.behandlingId) shouldBe oppgave
            repo.finnOppgaveFor(behandlingId = UUIDv7.ny()) shouldBe null
        }
    }

    private fun lagOppgave(
        tilstand: Oppgave.Tilstand.Type = KLAR_TIL_BEHANDLING,
        opprettet: ZonedDateTime = opprettetNå,
        saksbehandlerIdent: String? = null,
        person: Person = testPerson,
        behandling: Behandling = lagBehandling(person = person),
        emneknagger: Set<String> = emptySet(),
    ): Oppgave {
        return Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            ident = person.ident,
            saksbehandlerIdent = saksbehandlerIdent,
            behandlingId = behandling.behandlingId,
            opprettet = opprettet,
            emneknagger = emneknagger,
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
