package no.nav.dagpenger.saksbehandling.db.oppgave

import io.ktor.http.Parameters
import io.ktor.http.parseQueryString
import io.ktor.util.StringValues
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.Companion.søkbareTilstander
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.adressebeskyttelseTilganger
import java.time.LocalDate
import java.util.UUID

data class Søkefilter(
    val periode: Periode,
    val tilstander: Set<Tilstand.Type>,
    val saksbehandlerIdent: String? = null,
    val personIdent: String? = null,
    val oppgaveId: UUID? = null,
    val behandlingId: UUID? = null,
    val emneknagger: Set<String> = emptySet(),
    val emneknaggGruppertPerKategori: Map<String, Set<String>> = emptyMap(),
    val utløstAvTyper: Set<UtløstAvType> = emptySet(),
    val søknadId: UUID? = null,
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

            val tilstander = builder.tilstander() ?: søkbareTilstander
            val mineOppgaver = builder.mineOppgaver() ?: false
            val emneknagger = builder.emneknagg() ?: emptySet()
            val utløstAvTyper = builder.utløstAvTyper() ?: emptySet()
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
                emneknaggGruppertPerKategori = emneknagger.grupperEmneknaggPerKategori(),
                utløstAvTyper = utløstAvTyper,
                paginering = paginering,
            )
        }
    }
}

data class TildelNesteOppgaveFilter(
    val periode: Periode,
    val emneknagger: Set<String>,
    val emneknaggGruppertPerKategori: Map<String, Set<String>> = emptyMap(),
    val tilstander: Set<Tilstand.Type> = emptySet(),
    val utløstAvTyper: Set<UtløstAvType> = emptySet(),
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
            val utløstAvTyper = builder.utløstAvTyper() ?: emptySet()
            val tilstander = builder.tilstander() ?: emptySet()
            return TildelNesteOppgaveFilter(
                periode = Periode.fra(queryString),
                emneknagger = emneknagger,
                emneknaggGruppertPerKategori = emneknagger.grupperEmneknaggPerKategori(),
                tilstander = tilstander,
                utløstAvTyper = utløstAvTyper,
                egneAnsatteTilgang = egneAnsatteTilgang,
                adressebeskyttelseTilganger = adressebeskyttelseTilganger,
                harBeslutterRolle = harBeslutterRolle,
                navIdent = saksbehandler.navIdent,
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

    fun tilstander(): Set<Tilstand.Type>? = stringValues.getAll("tilstand")?.map { Tilstand.Type.valueOf(it) }?.toSet()

    fun utløstAvTyper(): Set<UtløstAvType>? = stringValues.getAll("utlostAv")?.map { UtløstAvType.valueOf(it) }?.toSet()
}

private fun Set<String>.grupperEmneknaggPerKategori(): Map<String, Set<String>> =
    this
        .groupBy { visningsNavn ->
            when {
                visningsNavn.startsWith("Ettersending") -> "ETTERSENDING"
                else -> hentKategoriForEmneknagg(visningsNavn)
            }
        }.mapValues { it.value.toSet() }

private fun hentKategoriForEmneknagg(visningsNavn: String): String {
    Emneknagg.Regelknagg.entries.find { it.visningsnavn == visningsNavn }?.let { regelknagg ->
        return when (regelknagg) {
            Emneknagg.Regelknagg.AVSLAG,
            Emneknagg.Regelknagg.INNVILGELSE,
            -> "SØKNADSRESULTAT"

            Emneknagg.Regelknagg.GJENOPPTAK -> "GJENOPPTAK"

            Emneknagg.Regelknagg.AVSLAG_MINSTEINNTEKT,
            Emneknagg.Regelknagg.AVSLAG_ARBEIDSINNTEKT,
            Emneknagg.Regelknagg.AVSLAG_ARBEIDSTID,
            Emneknagg.Regelknagg.AVSLAG_ALDER,
            Emneknagg.Regelknagg.AVSLAG_ANDRE_YTELSER,
            Emneknagg.Regelknagg.AVSLAG_STREIK,
            Emneknagg.Regelknagg.AVSLAG_OPPHOLD_UTLAND,
            Emneknagg.Regelknagg.AVSLAG_REELL_ARBEIDSSØKER,
            Emneknagg.Regelknagg.AVSLAG_IKKE_REGISTRERT,
            Emneknagg.Regelknagg.AVSLAG_UTESTENGT,
            Emneknagg.Regelknagg.AVSLAG_UTDANNING,
            Emneknagg.Regelknagg.AVSLAG_MEDLEMSKAP,
            -> "AVSLAGSGRUNN"

            Emneknagg.Regelknagg.RETTIGHET_ORDINÆR,
            Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT,
            Emneknagg.Regelknagg.RETTIGHET_PERMITTERT,
            Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK,
            Emneknagg.Regelknagg.RETTIGHET_KONKURS,
            -> "RETTIGHET"
        }
    }

    Emneknagg.PåVent.entries.find { it.visningsnavn == visningsNavn }?.let {
        return "PÅ_VENT"
    }

    Emneknagg.AvbrytBehandling.entries.find { it.visningsnavn == visningsNavn }?.let {
        return "AVBRUTT_GRUNN"
    }

    return "UDEFINERT"
}
