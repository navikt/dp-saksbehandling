package no.nav.dagpenger.saksbehandling.klage

interface KlageClient {
    suspend fun registrerKlage() {}
}