package no.nav.dagpenger.saksbehandling.statistikk

import io.ktor.http.Parameters
import io.ktor.http.parseQueryString
import io.ktor.util.StringValues
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.models.GrupperEtterDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode

data class StatistikkFilter(
    val periode: Periode,
    val statuser: Set<String> = emptySet(),
    val rettighetstyper: Set<String> = emptySet(),
    val utløstAvTyper: Set<UtløstAvType> = emptySet(),
    val grupperEtter: String = GrupperEtterDTO.OPPGAVETYPE.name,
) {
    companion object {
        fun fra(queryParameters: Parameters): StatistikkFilter {
            val builder = StatistikkFilterBuilder(queryParameters)

            val statuser = builder.status() ?: emptySet()
            val rettighetstyper = builder.rettighetstyper() ?: emptySet()
            val utløstAvTyper = builder.utløstAvTyper() ?: emptySet()
            val grupperEtter = builder.grupperEtter()

            return StatistikkFilter(
                periode = Periode.fra(queryParameters),
                statuser = statuser,
                rettighetstyper = rettighetstyper,
                utløstAvTyper = utløstAvTyper,
                grupperEtter = grupperEtter,
            )
        }
    }

    class StatistikkFilterBuilder {
        private val stringValues: StringValues

        constructor(queryString: String) {
            stringValues = parseQueryString(queryString)
        }

        constructor(stringValues: StringValues) {
            this.stringValues = stringValues
        }

        fun status(): Set<String>? = stringValues.getAll("tilstand")?.toSet()

        fun rettighetstyper(): Set<String>? = stringValues.getAll("rettighet")?.toSet()

        fun utløstAvTyper(): Set<UtløstAvType>? = stringValues.getAll("utlostAv")?.map { UtløstAvType.valueOf(it) }?.toSet()

        fun grupperEtter(): String = stringValues.get("grupperEtter") ?: GrupperEtterDTO.OPPGAVETYPE.name
    }
}
