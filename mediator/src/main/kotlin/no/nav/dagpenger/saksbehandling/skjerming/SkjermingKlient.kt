package no.nav.dagpenger.saksbehandling.skjerming

interface SkjermingKlient {
    suspend fun erSkjermetPerson(ident: String): Result<Boolean>
}
