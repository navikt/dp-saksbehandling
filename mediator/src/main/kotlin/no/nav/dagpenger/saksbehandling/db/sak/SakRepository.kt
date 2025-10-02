package no.nav.dagpenger.saksbehandling.db.sak

import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.SakHistorikk
import java.util.UUID

interface SakRepository {
    fun lagre(sakHistorikk: SakHistorikk)

    fun hentSakHistorikk(ident: String): SakHistorikk

    fun finnSakHistorikk(ident: String): SakHistorikk?

    fun hentSisteSakId(ident: String): UUID

    fun finnSisteSakId(ident: String): UUID?

    fun hentSakIdForBehandlingId(behandlingId: UUID): UUID

    fun hentDagpengerSakIdForBehandlingId(behandlingId: UUID): UUID

    fun lagreBehandling(
        personId: UUID,
        sakId: UUID,
        behandling: Behandling,
    )

    fun settArenaSakId(
        sakId: UUID,
        arenaSakId: String,
    )

    fun merkSakenSomDpSak(
        sakId: UUID,
        erDpSak: Boolean,
    )
}
