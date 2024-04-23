package no.nav.dagpenger.saksbehandling.db

import io.ktor.http.Parameters
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import java.time.LocalDate

data class Søkefilter(
    val periode: Periode,
    val tilstand: Oppgave.Tilstand.Type,
    val saksbehandlerIdent: String? = null,
) {
    companion object {
        val DEFAULT_SØKEFILTER = Søkefilter(
            periode = Periode.UBEGRENSET_PERIODE,
            tilstand = KLAR_TIL_BEHANDLING,
            saksbehandlerIdent = null,
        )

        fun fra(queryParamaters: Parameters, saksbehandlerIdent: String): Søkefilter {
            val tilstand =
                queryParamaters["tilstand"]?.let { Oppgave.Tilstand.Type.valueOf(it) } ?: KLAR_TIL_BEHANDLING

            val mine = queryParamaters["mine"]?.toBoolean() ?: false

            return Søkefilter(
                periode = Periode.fra(queryParamaters),
                tilstand = tilstand,
                saksbehandlerIdent = when {
                    mine -> saksbehandlerIdent
                    else -> null
                },
            )
        }
    }

    data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
    ) {
        init {
            require(fom.isBefore(tom)) { "Fom må være før tom" }
        }

        companion object {
            val MIN = LocalDate.of(1000, 1, 1)
            val MAX = LocalDate.of(3000, 1, 1)
            val UBEGRENSET_PERIODE = Periode(
                fom = MIN,
                tom = MAX,
            )

            fun fra(queryParamaters: Parameters): Periode {
                val fom = queryParamaters["fom"]?.let { LocalDate.parse(it) } ?: MIN
                val tom = queryParamaters["tom"]?.let { LocalDate.parse(it) } ?: MAX

                return Periode(
                    fom = fom,
                    tom = tom,
                )
            }
        }
    }
}
