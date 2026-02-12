package no.nav.dagpenger.saksbehandling.statistikk

import io.ktor.http.Parameters
import io.ktor.http.parseQueryString
import io.ktor.util.StringValues
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.models.GrupperEtterDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode

data class ProduksjonsstatistikkFilter(
    val periode: Periode,
    val tilstander: Set<Oppgave.Tilstand.Type> = emptySet(),
    val rettighetstyper: Set<String> = emptySet(),
    val utløstAvTyper: Set<UtløstAvType> = emptySet(),
    val grupperEtter: String = GrupperEtterDTO.OPPGAVETYPE.name,
) {
    companion object {
        fun fra(queryParameters: Parameters): ProduksjonsstatistikkFilter {
            val builder = StatistikkFilterBuilder(queryParameters)

            return ProduksjonsstatistikkFilter(
                periode = Periode.fra(queryParameters),
                tilstander = builder.tilstander(),
                rettighetstyper = builder.rettighetstyper(),
                utløstAvTyper = builder.utløstAvTyper(),
                grupperEtter = builder.grupperEtter(),
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

        fun tilstander(): Set<Oppgave.Tilstand.Type> =
            stringValues.getAll("tilstand")?.map { Oppgave.Tilstand.Type.valueOf(it) }?.toSet() ?: emptySet()

        fun rettighetstyper(): Set<String> = stringValues.getAll("rettighet")?.toSet() ?: emptySet()

        fun utløstAvTyper(): Set<UtløstAvType> = stringValues.getAll("utlostAv")?.map { UtløstAvType.valueOf(it) }?.toSet() ?: emptySet()

        fun grupperEtter(): String = stringValues.get("grupperEtter") ?: GrupperEtterDTO.OPPGAVETYPE.name
    }
}
