package no.nav.dagpenger.saksbehandling.skjerming

interface SkjermingKlient {
    suspend fun egenAnsatt(id: String): Result<Boolean>
}
