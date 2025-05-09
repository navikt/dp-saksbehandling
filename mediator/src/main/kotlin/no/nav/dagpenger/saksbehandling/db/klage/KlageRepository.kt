package no.nav.dagpenger.saksbehandling.db.klage

import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import java.util.UUID

interface KlageRepository {
    fun hentKlageBehandling(behandlingId: UUID): KlageBehandling

    fun lagre(klageBehandling: KlageBehandling)
}
