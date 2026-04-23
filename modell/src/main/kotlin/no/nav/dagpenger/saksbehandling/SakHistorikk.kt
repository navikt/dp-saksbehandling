package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.DpBehandlingOpprettetHendelse
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

    fun finnBehandling(behandlingId: UUID): Behandling? =
        saker
            .asSequence()
            .flatMap { it.behandlinger() }
            .firstOrNull { it.behandlingId == behandlingId }

    fun knyttTilSak(behandlingOpprettetHendelse: BehandlingOpprettetHendelse): KnyttTilSakResultat =
        saker
            .map {
                it.knyttTilSak(behandlingOpprettetHendelse)
            }.knyttTilSakResultat()

    fun knyttTilSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse): KnyttTilSakResultat =
        saker
            .map {
                it.knyttTilSak(søknadsbehandlingOpprettetHendelse)
            }.knyttTilSakResultat()

    fun knyttTilSak(hendelse: DpBehandlingOpprettetHendelse): KnyttTilSakResultat =
        saker
            .map {
                it.knyttTilSak(hendelse)
            }.knyttTilSakResultat()

    private fun List<KnyttTilSakResultat>.knyttTilSakResultat(): KnyttTilSakResultat {
        val sakerTilknyttet: List<KnyttTilSakResultat.KnyttetTilSak> =
            this.filterIsInstance<KnyttTilSakResultat.KnyttetTilSak>()
        return when (sakerTilknyttet.size) {
            0 -> {
                KnyttTilSakResultat.IkkeKnyttetTilSak(*saker.map { it.sakId }.toTypedArray())
            }

            1 -> {
                KnyttTilSakResultat.KnyttetTilSak(sakerTilknyttet.single().sak)
            }

            else -> {
                KnyttTilSakResultat.KnyttetTilFlereSaker(*sakerTilknyttet.map { it.sak.sakId }.toTypedArray())
            }
        }
    }

    fun leggTilSak(sak: Sak) = saker.add(sak)

    fun alleSaker(): List<Sak> = saker.toList()

    fun dagpengeSaker(): List<Sak> =
        saker.toList().filter { sak ->
            sak.behandlinger().none { behandling ->
                behandling.utløstAv == HendelseBehandler.DpBehandling.Ferietillegg
            }
        }

    fun ferietilleggSaker(): List<Sak> =
        saker.toList().filter { sak ->
            sak.behandlinger().any { behandling ->
                behandling.utløstAv == HendelseBehandler.DpBehandling.Ferietillegg
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SakHistorikk) return false
        if (this.person != other.person) return false
        if (this.alleSaker().sortedBy { it.sakId } != other.alleSaker().sortedBy { it.sakId }) return false

        return true
    }

    fun finnSak(function: (Sak) -> Boolean): Sak? = saker.firstOrNull(function)
}
