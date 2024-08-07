package no.nav.dagpenger.saksbehandling.adressebeskyttelse

internal enum class Gradering {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}

internal interface AdressebeskyttelseRepository {
    fun oppdaterAdressebeskyttetStatus(
        fnr: String,
        gradering: Gradering,
    ): Int
}
