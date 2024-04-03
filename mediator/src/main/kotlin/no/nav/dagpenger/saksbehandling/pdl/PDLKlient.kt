package no.nav.dagpenger.saksbehandling.pdl

interface PDLKlient {
    suspend fun erAdressebeskyttet(ident: String): Result<Boolean>

    suspend fun person(ident: String): Result<PDLPersonInfo>
}
