package no.nav.dagpenger.saksbehandling.statistikk

import io.ktor.http.Parameters
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode

data class StatistikkFilter(
    val periode: Periode,
) {
    companion object {
        fun fra(queryParameters: Parameters): StatistikkFilter =
            StatistikkFilter(
                periode = Periode.fra(queryParameters),
            )
    }
}
