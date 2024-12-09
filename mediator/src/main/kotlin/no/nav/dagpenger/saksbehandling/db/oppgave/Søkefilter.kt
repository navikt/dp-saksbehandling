package no.nav.dagpenger.saksbehandling.db.oppgave

import io.ktor.http.Parameters
import io.ktor.http.parseQueryString
import io.ktor.util.StringValues
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.Companion.søkbareTilstander
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.adressebeskyttelseTilganger
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

    fun paginering(): Søkefilter.Paginering {
        val antallOppgaver = stringValues["antallOppgaver"]
        val side = stringValues["side"]?.toInt()?.minus(1)
        return if (antallOppgaver == null || side == null) {
            Søkefilter.Paginering.DEFAULT
        } else {
            Søkefilter.Paginering(
                antallOppgaver.toInt(),
                side,
            )
        }
    }

    fun tilstand(): Set<Oppgave.Tilstand.Type>? {
        return stringValues.getAll("tilstand")?.map { Oppgave.Tilstand.Type.valueOf(it) }?.toSet()
    }
}

data class TildelNesteOppgaveFilter(
    val periode: Periode,
    val emneknagg: Set<String>,
    val egneAnsatteTilgang: Boolean = false,
    val adressebeskyttelseTilganger: Set<AdressebeskyttelseGradering>,
    val harBeslutterRolle: Boolean = false,
    val navIdent: String,
) {
    companion object {
        fun fra(
            queryString: String,
            saksbehandler: Saksbehandler,
        ): TildelNesteOppgaveFilter {
            val builder = FilterBuilder(queryString)
            val egneAnsatteTilgang = saksbehandler.tilganger.contains(TilgangType.EGNE_ANSATTE)
            val adressebeskyttelseTilganger = saksbehandler.adressebeskyttelseTilganger()
            val harBeslutterRolle: Boolean = saksbehandler.tilganger.contains(TilgangType.BESLUTTER)
            val emneknagger = builder.emneknagg() ?: emptySet()
            return TildelNesteOppgaveFilter(
                periode = Periode.fra(queryString),
                emneknagg = emneknagger,
                egneAnsatteTilgang = egneAnsatteTilgang,
                adressebeskyttelseTilganger = adressebeskyttelseTilganger,
                harBeslutterRolle = harBeslutterRolle,
                navIdent = saksbehandler.navIdent,
            )
        }
    }
}

data class Søkefilter(
    val periode: Periode,
    val tilstander: Set<Oppgave.Tilstand.Type>,
    val saksbehandlerIdent: String? = null,
    val personIdent: String? = null,
    val oppgaveId: UUID? = null,
    val behandlingId: UUID? = null,
    val emneknagger: Set<String> = emptySet(),
    val paginering: Paginering? = Paginering.DEFAULT,
) {
    data class Paginering(
        val antallOppgaver: Int,
        val side: Int,
    ) {
        init {
            require(antallOppgaver > 0) { "antallOppgaver må være større enn 0" }
            require(side >= 0) { "side må være større eller lik 0" }
        }

        companion object {
            val DEFAULT = Paginering(20, 0)
        }
    }

    companion object {
        fun fra(
            queryParameters: Parameters,
            saksbehandlerIdent: String,
        ): Søkefilter {
            val builder = FilterBuilder(queryParameters)

            val tilstander = builder.tilstand() ?: søkbareTilstander
            val mineOppgaver = builder.mineOppgaver() ?: false
            val emneknagger = builder.emneknagg() ?: emptySet()
            val paginering = builder.paginering()

            return Søkefilter(
                periode = Periode.fra(queryParameters),
                tilstander = tilstander,
                saksbehandlerIdent =
                    when {
                        mineOppgaver -> saksbehandlerIdent
                        else -> null
                    },
                emneknagger = emneknagger,
                paginering = paginering,
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
