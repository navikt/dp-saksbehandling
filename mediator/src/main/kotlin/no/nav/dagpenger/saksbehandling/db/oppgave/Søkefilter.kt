package no.nav.dagpenger.saksbehandling.db.oppgave

import io.ktor.http.Parameters
import io.ktor.util.StringValues
import io.ktor.util.StringValuesBuilderImpl
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.Companion.defaultOppgaveListTilstander
import java.time.LocalDate
import java.util.UUID

class FilterBuilder {
    private val stringValues: StringValues

    constructor(queryString: String) {
        stringValues = parseQueryString(queryString)
    }

    constructor(stringValues: StringValues) {
        this.stringValues = stringValues
    }

    fun fom(): LocalDate? = stringValues["fom"]?.let { LocalDate.parse(it) }

    fun tom(): LocalDate? = stringValues["tom"]?.let { LocalDate.parse(it) }

    fun emneknagg(): Set<String>? = stringValues.getAll("emneknagg")?.toSet()

    fun mineOppgaver(): Boolean? = stringValues["mineOppgaver"]?.toBoolean()

    fun tilstand(): Set<Oppgave.Tilstand.Type>? {
        return stringValues.getAll("tilstand")?.map { Oppgave.Tilstand.Type.valueOf(it) }?.toSet()
    }

    private fun parseQueryString(queryString: String): StringValues {
        if (queryString.isEmpty()) return StringValues.Empty

        val builder = StringValuesBuilderImpl()

        queryString
            .split("&")
            .map { it.split("=") }
            .map { it: List<String> -> builder.append(it[0], it[1]) }
        return builder.build()
    }
}

data class TildelNesteOppgaveFilter(
    val periode: Periode,
    val emneknagg: Set<String>,
    val harTilgangTilEgneAnsatte: Boolean = false,
    val harTilgangTilAdressebeskyttelser: Set<AdresseBeskyttelseGradering>,
) {
    companion object {
        fun fra(
            queryString: String,
            saksbehandlerTilgangEgneAnsatte: Boolean,
            adresseBeskyttelseGradering: Set<AdresseBeskyttelseGradering>,
        ): TildelNesteOppgaveFilter {
            val builder = FilterBuilder(queryString)

            return TildelNesteOppgaveFilter(
                periode = Periode.fra(queryString),
                emneknagg = builder.emneknagg() ?: emptySet(),
                harTilgangTilEgneAnsatte = saksbehandlerTilgangEgneAnsatte,
                harTilgangTilAdressebeskyttelser = adresseBeskyttelseGradering,
            )
        }
    }
}

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
        fun fra(
            queryParameters: Parameters,
            saksbehandlerIdent: String,
        ): Søkefilter {
            val builder = FilterBuilder(queryParameters)

            val tilstand = builder.tilstand() ?: defaultOppgaveListTilstander
            val mine = builder.mineOppgaver() ?: false
            val emneknagg = builder.emneknagg() ?: emptySet()

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
}

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    init {
        require(fom.isBefore(tom) || fom == tom) { "Fom må være før eller lik tom" }
    }

    companion object {
        val MIN: LocalDate = LocalDate.of(1000, 1, 1)
        val MAX: LocalDate = LocalDate.of(3000, 1, 1)
        val UBEGRENSET_PERIODE =
            Periode(
                fom = MIN,
                tom = MAX,
            )

        private fun fra(builder: FilterBuilder): Periode =
            Periode(
                fom = builder.fom() ?: MIN,
                tom = builder.tom() ?: MAX,
            )

        fun fra(queryString: String): Periode = fra(FilterBuilder(queryString))

        fun fra(queryParamaters: StringValues): Periode = fra(FilterBuilder(queryParamaters))
    }
}
