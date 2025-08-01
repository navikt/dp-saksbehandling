package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import java.util.UUID

data class SakHistorikk(
    val person: Person,
    private val saker: MutableSet<Sak> = mutableSetOf(),
) {
    companion object {
        fun rehydrer(
            person: Person,
            saker: Set<Sak>,
        ) = SakHistorikk(person = person).also {
            it.saker.addAll(saker)
        }
    }

    fun finnBehandling(behandlingId: UUID): Behandling? {
        return saker.asSequence()
            .flatMap { it.behandlinger() }
            .firstOrNull { it.behandlingId == behandlingId }
    }

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse) {
        saker.forEach { it.knyttTilSak(meldekortbehandlingOpprettetHendelse) }
    }

    fun knyttTilSak(manuellBehandlingOpprettetHendelse: ManuellBehandlingOpprettetHendelse) {
        saker.forEach { it.knyttTilSak(manuellBehandlingOpprettetHendelse) }
    }

    fun knyttTilSak(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        saker().single { it.sakId == behandlingOpprettetHendelse.sakId }.knyttTilSak(
            behandlingOpprettetHendelse = behandlingOpprettetHendelse,
        )
    }

    fun knyttTilSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        saker.forEach { it.knyttTilSak(søknadsbehandlingOpprettetHendelse) }
    }

    fun leggTilSak(sak: Sak) = saker.add(sak)

    fun saker(): List<Sak> = saker.toList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SakHistorikk) return false
        if (this.person != other.person) return false
        if (this.saker().sortedBy { it.sakId } != other.saker().sortedBy { it.sakId }) return false

        return true
    }
}
