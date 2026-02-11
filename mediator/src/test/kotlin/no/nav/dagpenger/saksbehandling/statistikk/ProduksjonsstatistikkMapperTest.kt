package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.models.StatistikkGruppeMedAntallDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkResultatSerieDTO
import no.nav.dagpenger.saksbehandling.statistikk.api.tilStatistikkResultatSerieDTOForRettighet
import no.nav.dagpenger.saksbehandling.statistikk.api.tilStatistikkResultatSerieDTOForUtløstAv
import no.nav.dagpenger.saksbehandling.statistikk.db.AntallOppgaverForTilstandOgRettighet
import no.nav.dagpenger.saksbehandling.statistikk.db.AntallOppgaverForTilstandOgUtløstAv
import org.junit.jupiter.api.Test

class ProduksjonsstatistikkMapperTest {
    @Test
    fun `Skal mappe til StatistikkV2DTO ved gruppering etter rettighet`() {
        val tilstanderOgRettighetAntall =
            listOf(
                AntallOppgaverForTilstandOgRettighet(
                    tilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                    rettighet = "Ordinær",
                    antall = 4,
                ),
                AntallOppgaverForTilstandOgRettighet(
                    tilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                    rettighet = "Verneplikt",
                    antall = 2,
                ),
                AntallOppgaverForTilstandOgRettighet(
                    tilstand = Oppgave.Tilstand.Type.PAA_VENT,
                    rettighet = "Permittert",
                    antall = 0,
                ),
            )
        val rettighetSerier =
            listOf(
                StatistikkResultatSerieDTO(
                    navn = "Ordinær",
                    verdier =
                        listOf(
                            StatistikkGruppeMedAntallDTO(
                                gruppe = "Klar til behandling",
                                antall = 4,
                            ),
                        ),
                ),
                StatistikkResultatSerieDTO(
                    navn = "Verneplikt",
                    verdier =
                        listOf(
                            StatistikkGruppeMedAntallDTO(
                                gruppe = "Klar til behandling",
                                antall = 2,
                            ),
                        ),
                ),
                StatistikkResultatSerieDTO(
                    navn = "Permittert",
                    verdier =
                        listOf(
                            StatistikkGruppeMedAntallDTO(
                                gruppe = "På vent",
                                antall = 0,
                            ),
                        ),
                ),
            )
        tilstanderOgRettighetAntall.tilStatistikkResultatSerieDTOForRettighet() shouldBe rettighetSerier
    }

    @Test
    fun `Skal mappe til StatistikkV2DTO ved gruppering etter UtløstAv`() {
        val tilstanderOgUtløstAvAntall =
            listOf(
                AntallOppgaverForTilstandOgUtløstAv(
                    tilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                    utløstAv = UtløstAvType.SØKNAD,
                    antall = 4,
                ),
                AntallOppgaverForTilstandOgUtløstAv(
                    tilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                    utløstAv = UtløstAvType.MANUELL,
                    antall = 2,
                ),
                AntallOppgaverForTilstandOgUtløstAv(
                    tilstand = Oppgave.Tilstand.Type.PAA_VENT,
                    utløstAv = UtløstAvType.SØKNAD,
                    antall = 0,
                ),
                AntallOppgaverForTilstandOgUtløstAv(
                    tilstand = Oppgave.Tilstand.Type.PAA_VENT,
                    utløstAv = UtløstAvType.MANUELL,
                    antall = 0,
                ),
                AntallOppgaverForTilstandOgUtløstAv(
                    tilstand = Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                    utløstAv = UtløstAvType.SØKNAD,
                    antall = 166,
                ),
                AntallOppgaverForTilstandOgUtløstAv(
                    tilstand = Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                    utløstAv = UtløstAvType.MANUELL,
                    antall = 67,
                ),
            )
        val utløstAvSerier =
            listOf(
                StatistikkResultatSerieDTO(
                    navn = "Søknad",
                    verdier =
                        listOf(
                            StatistikkGruppeMedAntallDTO(
                                gruppe = "Klar til behandling",
                                antall = 4,
                            ),
                            StatistikkGruppeMedAntallDTO(
                                gruppe = "På vent",
                                antall = 0,
                            ),
                            StatistikkGruppeMedAntallDTO(
                                gruppe = "Under behandling",
                                antall = 166,
                            ),
                        ),
                ),
                StatistikkResultatSerieDTO(
                    navn = "Manuell",
                    verdier =
                        listOf(
                            StatistikkGruppeMedAntallDTO(
                                gruppe = "Klar til behandling",
                                antall = 2,
                            ),
                            StatistikkGruppeMedAntallDTO(
                                gruppe = "På vent",
                                antall = 0,
                            ),
                            StatistikkGruppeMedAntallDTO(
                                gruppe = "Under behandling",
                                antall = 67,
                            ),
                        ),
                ),
            )
        tilstanderOgUtløstAvAntall.tilStatistikkResultatSerieDTOForUtløstAv() shouldBe utløstAvSerier
    }
}
