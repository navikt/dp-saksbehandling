package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.models.V2GruppeMedAntallDTO
import no.nav.dagpenger.saksbehandling.api.models.V2SerieDTO
import org.junit.jupiter.api.Test

class StatistikkV2MapperTest {
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
                V2SerieDTO(
                    navn = "Ordinær",
                    verdier =
                        listOf(
                            V2GruppeMedAntallDTO(
                                gruppe = "Klar til behandling",
                                antall = 4,
                            ),
                        ),
                ),
                V2SerieDTO(
                    navn = "Verneplikt",
                    verdier =
                        listOf(
                            V2GruppeMedAntallDTO(
                                gruppe = "Klar til behandling",
                                antall = 2,
                            ),
                        ),
                ),
                V2SerieDTO(
                    navn = "Permittert",
                    verdier =
                        listOf(
                            V2GruppeMedAntallDTO(
                                gruppe = "På vent",
                                antall = 0,
                            ),
                        ),
                ),
            )
        tilstanderOgRettighetAntall.tilDtoForRettighet() shouldBe rettighetSerier
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
                V2SerieDTO(
                    navn = "Søknad",
                    verdier =
                        listOf(
                            V2GruppeMedAntallDTO(
                                gruppe = "Klar til behandling",
                                antall = 4,
                            ),
                            V2GruppeMedAntallDTO(
                                gruppe = "På vent",
                                antall = 0,
                            ),
                            V2GruppeMedAntallDTO(
                                gruppe = "Under behandling",
                                antall = 166,
                            ),
                        ),
                ),
                V2SerieDTO(
                    navn = "Manuell",
                    verdier =
                        listOf(
                            V2GruppeMedAntallDTO(
                                gruppe = "Klar til behandling",
                                antall = 2,
                            ),
                            V2GruppeMedAntallDTO(
                                gruppe = "På vent",
                                antall = 0,
                            ),
                            V2GruppeMedAntallDTO(
                                gruppe = "Under behandling",
                                antall = 67,
                            ),
                        ),
                ),
            )
        tilstanderOgUtløstAvAntall.tilDtoForUtløstAv() shouldBe utløstAvSerier
    }
}
