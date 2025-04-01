package no.nav.dagpenger.saksbehandling

interface KlageMediator {
    fun hentKlage(klageId: java.util.UUID): no.nav.dagpenger.saksbehandling.api.models.KlageDTO
}
