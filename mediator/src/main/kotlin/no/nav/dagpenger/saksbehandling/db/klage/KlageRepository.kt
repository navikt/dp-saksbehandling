package no.nav.dagpenger.saksbehandling.db.klage

import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst.IkkeAktiv
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand
import java.time.LocalDateTime
import java.util.UUID

data class KlageBehandlingSammendrag(
    val behandlingId: UUID,
    val tilstand: String,
    val sistEndret: LocalDateTime,
)

interface KlageRepository {
    fun hentKlageBehandling(behandlingId: UUID): KlageBehandling

    fun hentBehandlingerIkkeFerdigstilt(
        type: KlageTilstand.Type,
        sistEndretEldreEnn: LocalDateTime,
    ): List<KlageBehandlingSammendrag>

    fun lagre(
        klageBehandling: KlageBehandling,
        ctx: Transaksjonskontekst = IkkeAktiv,
    )
}
