package no.nav.dagpenger.saksbehandling.statistikk.db

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.lagBehandling
import no.nav.dagpenger.saksbehandling.TestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType.INNSENDING
import no.nav.dagpenger.saksbehandling.UtløstAvType.KLAGE
import no.nav.dagpenger.saksbehandling.UtløstAvType.MANUELL
import no.nav.dagpenger.saksbehandling.UtløstAvType.MELDEKORT
import no.nav.dagpenger.saksbehandling.UtløstAvType.SØKNAD
import no.nav.dagpenger.saksbehandling.api.models.GrupperEtterDTO
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.statistikk.ProduksjonsstatistikkFilter
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.sql.DataSource

class PostgresProduksjonsstatistikkRepositoryTest {
    @Test
    fun `Hent produksjonsstatistikk basert på filter`() {
        val iDag = TestHelper.opprettetNå
        val iGår = TestHelper.opprettetNå.minusDays(1)
        val periodeFomIGårTomIDag =
            Periode(
                fom = iGår.toLocalDate(),
                tom = LocalDate.now().plusDays(1),
            )
        val filterPeriodeFomIGårTomIDag = ProduksjonsstatistikkFilter(periode = periodeFomIGårTomIDag)
        val behandling1 = lagBehandling(opprettet = iGår)
        val behandling2 = lagBehandling(opprettet = iDag)
        val behandling3 = lagBehandling(opprettet = iDag, utløstAvType = KLAGE)
        val behandling4 = lagBehandling(opprettet = iDag)
        val behandling5 = lagBehandling(opprettet = iDag)
        val behandling6 = lagBehandling(opprettet = iGår.minusDays(1))

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
        val oppgave6FerdigBehandlet =
            lagOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = behandling6.opprettet,
                tilstand = Oppgave.FerdigBehandlet,
                behandling = behandling6,
                emneknagger = setOf("Ordinær"),
            )
        DBTestHelper.withBehandlinger(
            behandlinger = listOf(behandling1, behandling2, behandling3, behandling4, behandling5, behandling6),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgave1FerdigBehandlet)
            repo.lagre(oppgave2FerdigBehandlet)
            repo.lagre(oppgave3FerdigBehandlet)
            repo.lagre(oppgave4KlarTilBehandling)
            repo.lagre(oppgave5KlarTilKontroll)
            repo.lagre(oppgave6FerdigBehandlet)

            val statistikkTjeneste = PostgresProduksjonsstatistikkRepository(ds)

            val tilstanderAlle =
                statistikkTjeneste.hentTilstanderMedUtløstAvFilter(
                    produksjonsstatistikkFilter = filterPeriodeFomIGårTomIDag,
                )

            tilstanderAlle.size shouldBe 7
            tilstanderAlle.single { it.tilstand == KLAR_TIL_BEHANDLING }.antall shouldBe 1
            tilstanderAlle.single { it.tilstand == KLAR_TIL_BEHANDLING }.eldsteOppgaveTidspunkt!! shouldBe behandling4.opprettet
            tilstanderAlle.single { it.tilstand == UNDER_BEHANDLING }.antall shouldBe 0
            tilstanderAlle.single { it.tilstand == PAA_VENT }.antall shouldBe 0
            tilstanderAlle.single { it.tilstand == KLAR_TIL_KONTROLL }.antall shouldBe 1
            tilstanderAlle.single { it.tilstand == KLAR_TIL_KONTROLL }.eldsteOppgaveTidspunkt!! shouldBe behandling5.opprettet
            tilstanderAlle.single { it.tilstand == UNDER_KONTROLL }.antall shouldBe 0
            tilstanderAlle.single { it.tilstand == FERDIG_BEHANDLET }.antall shouldBe 3
            tilstanderAlle.single { it.tilstand == FERDIG_BEHANDLET }.eldsteOppgaveTidspunkt!! shouldBe behandling1.opprettet
            tilstanderAlle.single { it.tilstand == AVBRUTT }.antall shouldBe 0

            val tilstanderKlage =
                statistikkTjeneste.hentTilstanderMedUtløstAvFilter(
                    ProduksjonsstatistikkFilter(
                        periode = periodeFomIGårTomIDag,
                        utløstAvTyper = setOf(KLAGE),
                    ),
                )

            tilstanderKlage.size shouldBe 7
            tilstanderKlage.single { it.tilstand == KLAR_TIL_BEHANDLING }.antall shouldBe 0
            tilstanderKlage.single { it.tilstand == UNDER_BEHANDLING }.antall shouldBe 0
            tilstanderKlage.single { it.tilstand == PAA_VENT }.antall shouldBe 0
            tilstanderKlage.single { it.tilstand == KLAR_TIL_KONTROLL }.antall shouldBe 0
            tilstanderKlage.single { it.tilstand == UNDER_KONTROLL }.antall shouldBe 0
            tilstanderKlage.single { it.tilstand == FERDIG_BEHANDLET }.antall shouldBe 1
            tilstanderKlage.single { it.tilstand == FERDIG_BEHANDLET }.eldsteOppgaveTidspunkt!! shouldBe behandling3.opprettet
            tilstanderKlage.single { it.tilstand == AVBRUTT }.antall shouldBe 0

            val tilstanderSøknadAlle = statistikkTjeneste.hentTilstanderMedRettighetFilter(filterPeriodeFomIGårTomIDag)

            tilstanderSøknadAlle.size shouldBe 7
            tilstanderSøknadAlle.single { it.tilstand == KLAR_TIL_BEHANDLING }.antall shouldBe 1
            tilstanderSøknadAlle.single { it.tilstand == KLAR_TIL_BEHANDLING }.eldsteOppgaveTidspunkt!! shouldBe behandling4.opprettet
            tilstanderSøknadAlle.single { it.tilstand == UNDER_BEHANDLING }.antall shouldBe 0
            tilstanderSøknadAlle.single { it.tilstand == PAA_VENT }.antall shouldBe 0
            tilstanderSøknadAlle.single { it.tilstand == KLAR_TIL_KONTROLL }.antall shouldBe 1
            tilstanderSøknadAlle.single { it.tilstand == KLAR_TIL_KONTROLL }.eldsteOppgaveTidspunkt!! shouldBe behandling5.opprettet
            tilstanderSøknadAlle.single { it.tilstand == UNDER_KONTROLL }.antall shouldBe 0
            tilstanderSøknadAlle.single { it.tilstand == FERDIG_BEHANDLET }.antall shouldBe 2
            tilstanderSøknadAlle.single { it.tilstand == FERDIG_BEHANDLET }.eldsteOppgaveTidspunkt!! shouldBe behandling1.opprettet
            tilstanderSøknadAlle.single { it.tilstand == AVBRUTT }.antall shouldBe 0

            val tilstanderSøknadVerneplikt =
                statistikkTjeneste.hentTilstanderMedRettighetFilter(
                    ProduksjonsstatistikkFilter(
                        periode = periodeFomIGårTomIDag,
                        rettighetstyper = setOf("Verneplikt"),
                    ),
                )

            tilstanderSøknadVerneplikt.size shouldBe 7
            tilstanderSøknadVerneplikt.single { it.tilstand == KLAR_TIL_BEHANDLING }.antall shouldBe 0
            tilstanderSøknadVerneplikt.single { it.tilstand == UNDER_BEHANDLING }.antall shouldBe 0
            tilstanderSøknadVerneplikt.single { it.tilstand == PAA_VENT }.antall shouldBe 0
            tilstanderSøknadVerneplikt.single { it.tilstand == KLAR_TIL_KONTROLL }.antall shouldBe 1
            tilstanderSøknadVerneplikt.single { it.tilstand == KLAR_TIL_KONTROLL }.eldsteOppgaveTidspunkt!! shouldBe behandling5.opprettet
            tilstanderSøknadVerneplikt.single { it.tilstand == UNDER_KONTROLL }.antall shouldBe 0
            tilstanderSøknadVerneplikt.single { it.tilstand == FERDIG_BEHANDLET }.antall shouldBe 1
            tilstanderSøknadVerneplikt.single { it.tilstand == FERDIG_BEHANDLET }.eldsteOppgaveTidspunkt!! shouldBe behandling2.opprettet
            tilstanderSøknadVerneplikt.single { it.tilstand == AVBRUTT }.antall shouldBe 0

            val utløstAvAlle = statistikkTjeneste.hentUtløstAvMedTilstandFilter(filterPeriodeFomIGårTomIDag)

            utløstAvAlle.size shouldBe 5
            utløstAvAlle.single { it.utløstAv == SØKNAD }.antall shouldBe 4
            utløstAvAlle.single { it.utløstAv == KLAGE }.antall shouldBe 1
            utløstAvAlle.single { it.utløstAv == INNSENDING }.antall shouldBe 0
            utløstAvAlle.single { it.utløstAv == MELDEKORT }.antall shouldBe 0
            utløstAvAlle.single { it.utløstAv == MANUELL }.antall shouldBe 0
//            utløstAvAlle.single { it.utløstAv == OMGJØRING }.antall shouldBe 0

            val utløstAvFerdigBehandlet =
                statistikkTjeneste.hentUtløstAvMedTilstandFilter(
                    produksjonsstatistikkFilter =
                        ProduksjonsstatistikkFilter(
                            periode = periodeFomIGårTomIDag,
                            tilstander = setOf(FERDIG_BEHANDLET),
                        ),
                )

            utløstAvFerdigBehandlet.size shouldBe 5
            utløstAvFerdigBehandlet.single { it.utløstAv == SØKNAD }.antall shouldBe 2
            utløstAvFerdigBehandlet.single { it.utløstAv == KLAGE }.antall shouldBe 1
            utløstAvFerdigBehandlet.single { it.utløstAv == INNSENDING }.antall shouldBe 0
//            utløstAvFerdigBehandlet.single { it.utløstAv == OMGJØRING }.antall shouldBe 0
            utløstAvFerdigBehandlet.single { it.utløstAv == MELDEKORT }.antall shouldBe 0
            utløstAvFerdigBehandlet.single { it.utløstAv == MANUELL }.antall shouldBe 0

            val resultatSerieForUtløstAv =
                statistikkTjeneste.hentResultatSerierForUtløstAv(
                    produksjonsstatistikkFilter =
                        ProduksjonsstatistikkFilter(
                            periode = periodeFomIGårTomIDag,
                            tilstander = setOf(FERDIG_BEHANDLET, PAA_VENT, KLAR_TIL_KONTROLL),
                            utløstAvTyper = setOf(SØKNAD, KLAGE),
                            grupperEtter = GrupperEtterDTO.OPPGAVETYPE.name,
                        ),
                )
            resultatSerieForUtløstAv.size shouldBe 6
            resultatSerieForUtløstAv.single { it.tilstand == FERDIG_BEHANDLET && it.utløstAv == SØKNAD }.antall shouldBe 2
            resultatSerieForUtløstAv.single { it.tilstand == PAA_VENT && it.utløstAv == SØKNAD }.antall shouldBe 0
            resultatSerieForUtløstAv.single { it.tilstand == KLAR_TIL_KONTROLL && it.utløstAv == SØKNAD }.antall shouldBe 1
            resultatSerieForUtløstAv.single { it.tilstand == FERDIG_BEHANDLET && it.utløstAv == KLAGE }.antall shouldBe 1
            resultatSerieForUtløstAv.single { it.tilstand == PAA_VENT && it.utløstAv == KLAGE }.antall shouldBe 0
            resultatSerieForUtløstAv.single { it.tilstand == KLAR_TIL_KONTROLL && it.utløstAv == KLAGE }.antall shouldBe 0

            val resultatSerieForRettigheter =
                statistikkTjeneste.hentResultatSerierForRettigheter(
                    produksjonsstatistikkFilter =
                        ProduksjonsstatistikkFilter(
                            periode = periodeFomIGårTomIDag,
                            tilstander = setOf(FERDIG_BEHANDLET, KLAR_TIL_BEHANDLING, KLAR_TIL_KONTROLL),
                            rettighetstyper = setOf("Verneplikt"),
                            grupperEtter = GrupperEtterDTO.RETTIGHETSTYPE.name,
                        ),
                )
            resultatSerieForRettigheter.size shouldBe 3
            resultatSerieForRettigheter.single { it.tilstand == FERDIG_BEHANDLET && it.rettighet == "Verneplikt" }.antall shouldBe 1
            resultatSerieForRettigheter.single { it.tilstand == KLAR_TIL_BEHANDLING && it.rettighet == "Verneplikt" }.antall shouldBe 0
            resultatSerieForRettigheter.single { it.tilstand == KLAR_TIL_KONTROLL && it.rettighet == "Verneplikt" }.antall shouldBe 1

            val resultatSerieForFlereRettigheter =
                statistikkTjeneste.hentResultatSerierForRettigheter(
                    produksjonsstatistikkFilter =
                        ProduksjonsstatistikkFilter(
                            periode = periodeFomIGårTomIDag,
                            tilstander = setOf(FERDIG_BEHANDLET),
                            rettighetstyper = setOf("Verneplikt", "MikkeMus"),
                            grupperEtter = GrupperEtterDTO.RETTIGHETSTYPE.name,
                        ),
                )
            resultatSerieForFlereRettigheter.size shouldBe 2
            resultatSerieForFlereRettigheter.single { it.tilstand == FERDIG_BEHANDLET && it.rettighet == "MikkeMus" }.antall shouldBe 1
            resultatSerieForFlereRettigheter.single { it.tilstand == FERDIG_BEHANDLET && it.rettighet == "Verneplikt" }.antall shouldBe 1
        }
    }

    @Test
    fun `test hentAntallVedtakGjort`() {
        val behandling1 = lagBehandling(behandlingId = UUIDv7.ny())
        val behandling2 = lagBehandling(behandlingId = UUIDv7.ny())
        val behandling3 = lagBehandling(behandlingId = UUIDv7.ny())
        DBTestHelper.withBehandlinger(
            behandlinger = listOf(behandling1, behandling2, behandling3),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling1,
                ),
            )
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling2,
                ),
            )
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling3,
                ),
            )

            val produksjonsstatistikkRepository = PostgresProduksjonsstatistikkRepository(ds)
            val result = produksjonsstatistikkRepository.hentAntallVedtakGjort()

            result.dag shouldBe 3
            result.uke shouldBe 3
            result.totalt shouldBe 3
        }
    }

    @Test
    fun `test hentBeholdningsInfo`() {
        val behandling1 = lagBehandling()
        val behandling2 = lagBehandling()
        DBTestHelper.withBehandlinger(
            behandlinger = listOf(behandling1, behandling2),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(lagOppgave(tilstand = Oppgave.KlarTilBehandling, behandling = behandling1))
            repo.lagre(lagOppgave(tilstand = Oppgave.KlarTilBehandling, behandling = behandling2))

            val produksjonsstatistikkRepository = PostgresProduksjonsstatistikkRepository(ds)
            val result = produksjonsstatistikkRepository.hentBeholdningsInfo()

            result.antallOppgaverKlarTilBehandling shouldBe 2
        }
    }
}
