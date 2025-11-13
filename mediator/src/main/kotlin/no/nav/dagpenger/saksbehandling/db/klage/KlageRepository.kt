package no.nav.dagpenger.saksbehandling.db.klage

import no.nav.dagpenger.saksbehandling.klage.Klage
import java.util.UUID

interface KlageRepository {
    fun hentKlageBehandling(behandlingId: UUID): Klage

    fun lagre(klage: Klage)
}
