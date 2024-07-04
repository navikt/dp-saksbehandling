package no.nav.dagpenger.saksbehandling.skjerming

internal interface SkjermingRepository {
    fun oppdaterSkjermingStatus(
        fnr: String,
        skjermet: Boolean,
    ): Int
}
