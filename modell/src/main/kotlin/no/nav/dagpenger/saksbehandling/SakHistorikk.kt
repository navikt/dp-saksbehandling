package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import java.util.UUID

data class SakHistorikk(
    val person: Person,
    private val saker: MutableSet<NySak> = mutableSetOf(),
) {
    companion object {
        fun rehydrer(
            person: Person,
            saker: Set<NySak>,
        ) = SakHistorikk(person = person).also {
            it.saker.addAll(saker)
        }
    }

    fun finnBehandling(behandinId: UUID): Behandling? {
        return saker.asSequence()
            .flatMap { it.behandlinger() }
            .firstOrNull { it.behandlingId == behandinId }
    }

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse) {
        saker.forEach { it.knyttTilSak(meldekortbehandlingOpprettetHendelse) }
    }

    fun knyttTilSak(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        saker().single { it.sakId == behandlingOpprettetHendelse.sakId }.knyttTilSak(
            behandlingOpprettetHendelse = behandlingOpprettetHendelse,
        )
    }

    fun leggTilSak(sak: NySak) = saker.add(sak)

    fun saker(): List<NySak> = saker.toList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SakHistorikk) return false
        if (this.person != other.person) return false
        if (this.saker().sortedBy { it.sakId } != other.saker().sortedBy { it.sakId }) return false

        return true
    }
}
