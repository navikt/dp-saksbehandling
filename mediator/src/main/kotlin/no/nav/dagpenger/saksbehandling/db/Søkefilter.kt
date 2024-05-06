package no.nav.dagpenger.saksbehandling.db

import io.ktor.http.Parameters
import no.nav.dagpenger.saksbehandling.Oppgave
import java.time.LocalDate
import java.util.UUID

data class Søkefilter(
    val periode: Periode,
    val tilstand: Set<Oppgave.Tilstand.Type>,
    val saksbehandlerIdent: String? = null,
    val personIdent: String? = null,
    val oppgaveId: UUID? = null,
    val behandlingId: UUID? = null,
    val emneknagg: Set<String> = emptySet(),
) {
    companion object {
        val DEFAULT_SØKEFILTER =
            Søkefilter(
                periode = Periode.UBEGRENSET_PERIODE,
                tilstand = Oppgave.Tilstand.Type.values,
                saksbehandlerIdent = null,
                personIdent = null,
                oppgaveId = null,
                behandlingId = null,
            )

        fun fra(
            queryParameters: Parameters,
            saksbehandlerIdent: String,
        ): Søkefilter {
            val tilstand =
                queryParameters.getAll("tilstand")?.map { Oppgave.Tilstand.Type.valueOf(it) }?.toSet()
                    ?: Oppgave.Tilstand.Type.values

            val mine = queryParameters["mineOppgaver"]?.toBoolean() ?: false
            val emneknagg = queryParameters.getAll("emneknagg")?.toSet() ?: emptySet()

            return Søkefilter(
                periode = Periode.fra(queryParameters),
                tilstand = tilstand,
                saksbehandlerIdent =
                    when {
                        mine -> saksbehandlerIdent
                        else -> null
                    },
                emneknagg = emneknagg,
            )
        }
    }

    data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
    ) {
        init {
            require(fom.isBefore(tom) || fom.equals(tom)) { "Fom må være før eller lik tom" }
        }

        companion object {
            val MIN = LocalDate.of(1000, 1, 1)
            val MAX = LocalDate.of(3000, 1, 1)
            val UBEGRENSET_PERIODE =
                Periode(
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
