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

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse): KnyttTilSakResultat {
        return saker.map { it.knyttTilSak(meldekortbehandlingOpprettetHendelse) }.knyttTilSakResultat()
    }

    fun knyttTilSak(manuellBehandlingOpprettetHendelse: ManuellBehandlingOpprettetHendelse): KnyttTilSakResultat {
        return saker.map { it.knyttTilSak(manuellBehandlingOpprettetHendelse) }.knyttTilSakResultat()
    }

    fun knyttTilSak(behandlingOpprettetHendelse: BehandlingOpprettetHendelse): KnyttTilSakResultat {
        return saker.map { it.knyttTilSak(behandlingOpprettetHendelse) }.knyttTilSakResultat()
    }

    fun knyttTilSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse): KnyttTilSakResultat {
        return saker.map { it.knyttTilSak(søknadsbehandlingOpprettetHendelse) }.knyttTilSakResultat()
    }

    private fun List<KnyttTilSakResultat>.knyttTilSakResultat(): KnyttTilSakResultat {
        val sakerTilKnyttet: List<KnyttTilSakResultat.KnyttetTilSak> = this.filterIsInstance<KnyttTilSakResultat.KnyttetTilSak>()
        return when (sakerTilKnyttet.size) {
            0 -> {
                KnyttTilSakResultat.IkkeKnyttetTilSak(*saker.map { it.sakId }.toTypedArray())
            }

            1 -> {
                KnyttTilSakResultat.KnyttetTilSak(sakerTilKnyttet.single().sak)
            }

            else -> {
                KnyttTilSakResultat.KnyttetTilFlereSaker(*sakerTilKnyttet.map { it.sak.sakId }.toTypedArray())
            }
        }
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
