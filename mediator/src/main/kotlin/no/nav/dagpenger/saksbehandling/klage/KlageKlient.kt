package no.nav.dagpenger.saksbehandling.klage

interface KlageKlient {
    suspend fun registrerKlage() {}
}
