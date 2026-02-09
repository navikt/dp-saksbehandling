package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.lagBehandling
import no.nav.dagpenger.saksbehandling.TestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.test.Test

class PostgresStatistikkV2TjenesteTest {
    @Test
    fun `Hent alt fra statistikkv2`() {
        val iDag = TestHelper.opprettetNå
        val iGår = TestHelper.opprettetNå.minusDays(1)
        val periode =
            Periode(
                fom = iGår.toLocalDate(),
                tom = LocalDate.now().plusDays(1),
            )
        val defaultfilter = StatistikkFilter(periode = periode)
        val behandling1 = lagBehandling(opprettet = iGår)
        val behandling2 = lagBehandling(opprettet = iDag)
        val behandling3 = lagBehandling(opprettet = iDag, utløstAvType = UtløstAvType.KLAGE)
        val behandling4 = lagBehandling(opprettet = iDag)
        val behandling5 = lagBehandling(opprettet = iDag)

        val oppgave1FerdigBehandlet =
            lagOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = behandling1.opprettet,
                tilstand = Oppgave.FerdigBehandlet,
                behandling = behandling1,
                emneknagger = setOf("Ordinær"),
            )
        val oppgave2FerdigBehandlet =
            lagOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = behandling2.opprettet,
                tilstand = Oppgave.FerdigBehandlet,
                behandling = behandling2,
                emneknagger = setOf("Verneplikt", "MikkeMus"),
            )
        val oppgave3FerdigBehandlet =
            lagOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = behandling3.opprettet,
                tilstand = Oppgave.FerdigBehandlet,
                behandling = behandling3,
                emneknagger = setOf("Ordinær"),
            )
        val oppgave4KlarTilBehandling =
            lagOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = behandling4.opprettet,
                tilstand = Oppgave.KlarTilBehandling,
                behandling = behandling4,
                emneknagger = setOf("Ordinær"),
            )
        val oppgave5KlarTilKontroll =
            lagOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = behandling5.opprettet,
                tilstand = Oppgave.KlarTilKontroll,
                behandling = behandling5,
                emneknagger = setOf("Verneplikt"),
            )
        DBTestHelper.withBehandlinger(
            behandlinger = listOf(behandling1, behandling2, behandling3, behandling4, behandling5),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgave1FerdigBehandlet)
            repo.lagre(oppgave2FerdigBehandlet)
            repo.lagre(oppgave3FerdigBehandlet)
            repo.lagre(oppgave4KlarTilBehandling)
            repo.lagre(oppgave5KlarTilKontroll)

            val statistikkTjeneste = PostgresStatistikkV2Tjeneste(ds)

            val tilstanderAlle = statistikkTjeneste.hentTilstanderMedUtløstAvFilter(statistikkFilter = defaultfilter)

            tilstanderAlle.size shouldBe 7
            tilstanderAlle.single { it.navn == "KLAR_TIL_BEHANDLING" }.total shouldBe 1
            tilstanderAlle.single { it.navn == "KLAR_TIL_BEHANDLING" }.eldsteOppgave!! shouldBe behandling4.opprettet
            tilstanderAlle.single { it.navn == "UNDER_BEHANDLING" }.total shouldBe 0
            tilstanderAlle.single { it.navn == "PAA_VENT" }.total shouldBe 0
            tilstanderAlle.single { it.navn == "KLAR_TIL_KONTROLL" }.total shouldBe 1
            tilstanderAlle.single { it.navn == "KLAR_TIL_KONTROLL" }.eldsteOppgave!! shouldBe behandling5.opprettet
            tilstanderAlle.single { it.navn == "UNDER_KONTROLL" }.total shouldBe 0
            tilstanderAlle.single { it.navn == "FERDIG_BEHANDLET" }.total shouldBe 3
            tilstanderAlle.single { it.navn == "FERDIG_BEHANDLET" }.eldsteOppgave!! shouldBe behandling1.opprettet
            tilstanderAlle.single { it.navn == "AVBRUTT" }.total shouldBe 0

            val tilstanderKlage =
                statistikkTjeneste.hentTilstanderMedUtløstAvFilter(
                    StatistikkFilter(
                        periode = periode,
                        utløstAvTyper = setOf(UtløstAvType.KLAGE),
                    ),
                )

            tilstanderKlage.size shouldBe 7
            tilstanderKlage.single { it.navn == "KLAR_TIL_BEHANDLING" }.total shouldBe 0
            tilstanderKlage.single { it.navn == "UNDER_BEHANDLING" }.total shouldBe 0
            tilstanderKlage.single { it.navn == "PAA_VENT" }.total shouldBe 0
            tilstanderKlage.single { it.navn == "KLAR_TIL_KONTROLL" }.total shouldBe 0
            tilstanderKlage.single { it.navn == "UNDER_KONTROLL" }.total shouldBe 0
            tilstanderKlage.single { it.navn == "FERDIG_BEHANDLET" }.total shouldBe 1
            tilstanderKlage.single { it.navn == "FERDIG_BEHANDLET" }.eldsteOppgave!! shouldBe behandling3.opprettet
            tilstanderKlage.single { it.navn == "AVBRUTT" }.total shouldBe 0

            val tilstanderSøknad = statistikkTjeneste.hentTilstanderMedRettighetFilter(StatistikkFilter(periode))

            tilstanderSøknad.size shouldBe 7
            tilstanderSøknad.single { it.navn == "KLAR_TIL_BEHANDLING" }.total shouldBe 1
            tilstanderSøknad.single { it.navn == "KLAR_TIL_BEHANDLING" }.eldsteOppgave!! shouldBe behandling4.opprettet
            tilstanderSøknad.single { it.navn == "UNDER_BEHANDLING" }.total shouldBe 0
            tilstanderSøknad.single { it.navn == "PAA_VENT" }.total shouldBe 0
            tilstanderSøknad.single { it.navn == "KLAR_TIL_KONTROLL" }.total shouldBe 1
            tilstanderSøknad.single { it.navn == "KLAR_TIL_KONTROLL" }.eldsteOppgave!! shouldBe behandling5.opprettet
            tilstanderSøknad.single { it.navn == "UNDER_KONTROLL" }.total shouldBe 0
            tilstanderSøknad.single { it.navn == "FERDIG_BEHANDLET" }.total shouldBe 2
            tilstanderSøknad.single { it.navn == "FERDIG_BEHANDLET" }.eldsteOppgave!! shouldBe behandling1.opprettet
            tilstanderSøknad.single { it.navn == "AVBRUTT" }.total shouldBe 0

            val tilstanderVerneplikt =
                statistikkTjeneste.hentTilstanderMedRettighetFilter(
                    StatistikkFilter(
                        periode = periode,
                        rettighetstyper = setOf("Verneplikt"),
                    ),
                )

            tilstanderVerneplikt.size shouldBe 7
            tilstanderVerneplikt.single { it.navn == "KLAR_TIL_BEHANDLING" }.total shouldBe 0
            tilstanderVerneplikt.single { it.navn == "UNDER_BEHANDLING" }.total shouldBe 0
            tilstanderVerneplikt.single { it.navn == "PAA_VENT" }.total shouldBe 0
            tilstanderVerneplikt.single { it.navn == "KLAR_TIL_KONTROLL" }.total shouldBe 1
            tilstanderVerneplikt.single { it.navn == "KLAR_TIL_KONTROLL" }.eldsteOppgave!! shouldBe behandling5.opprettet
            tilstanderVerneplikt.single { it.navn == "UNDER_KONTROLL" }.total shouldBe 0
            tilstanderVerneplikt.single { it.navn == "FERDIG_BEHANDLET" }.total shouldBe 1
            tilstanderVerneplikt.single { it.navn == "FERDIG_BEHANDLET" }.eldsteOppgave!! shouldBe behandling2.opprettet
            tilstanderVerneplikt.single { it.navn == "AVBRUTT" }.total shouldBe 0
        }

        // TODO: Test henting av serier
    }
}
