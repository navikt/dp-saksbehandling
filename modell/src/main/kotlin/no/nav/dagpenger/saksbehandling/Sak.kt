package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.DpBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import java.time.LocalDateTime
import java.util.UUID

sealed class KnyttTilSakResultat {
    data class KnyttetTilSak(
        val sak: Sak,
    ) : KnyttTilSakResultat()

    data class IkkeKnyttetTilSak(
        val sakIder: Set<UUID>,
    ) : KnyttTilSakResultat() {
        constructor(vararg sakId: UUID) : this(sakId.toSet())
    }

    data class KnyttetTilFlereSaker(
        val sakIder: Set<UUID>,
    ) : KnyttTilSakResultat() {
        constructor(vararg sakId: UUID) : this(sakId.toSet())
    }
}

data class Sak(
    val sakId: UUID = UUIDv7.ny(),
    val opprettet: LocalDateTime,
    private val behandlinger: MutableSet<Behandling> = mutableSetOf(),
) {
    fun behandlinger(): List<Behandling> = behandlinger.toList()

    fun leggTilBehandling(behandling: Behandling) = behandlinger.add(behandling)

    fun erFerietilleggsSak(): Boolean = behandlinger.any { it.utløstAv == HendelseBehandler.DpBehandling.Ferietillegg }

    private fun basertPåBehandlingErKnyttetTilSak(basertPåBehandlingId: UUID?): Boolean =
        this.behandlinger
            .map { it.behandlingId }
            .contains(basertPåBehandlingId)

    fun knyttTilSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse): KnyttTilSakResultat =
        if (this.sakId == søknadsbehandlingOpprettetHendelse.behandlingskjedeId ||
            this.basertPåBehandlingErKnyttetTilSak(
                søknadsbehandlingOpprettetHendelse.basertPåBehandling,
            )
        ) {
            behandlinger.add(
                Behandling(
                    behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
                    utløstAv = HendelseBehandler.DpBehandling.Søknad,
                    opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
                    hendelse = søknadsbehandlingOpprettetHendelse,
                ),
            )
            KnyttTilSakResultat.KnyttetTilSak(this)
        } else {
            KnyttTilSakResultat.IkkeKnyttetTilSak(this.sakId)
        }

    fun knyttTilSak(hendelse: DpBehandlingOpprettetHendelse): KnyttTilSakResultat =
        if (this.sakId == hendelse.behandlingskjedeId ||
            this.basertPåBehandlingErKnyttetTilSak(hendelse.basertPåBehandling)
        ) {
            behandlinger.add(
                Behandling(
                    behandlingId = hendelse.behandlingId,
                    utløstAv = hendelse.type,
                    opprettet = hendelse.opprettet,
                    hendelse = hendelse,
                ),
            )
            KnyttTilSakResultat.KnyttetTilSak(this)
        } else {
            KnyttTilSakResultat.IkkeKnyttetTilSak(this.sakId)
        }

    fun knyttTilSak(behandlingOpprettetHendelse: BehandlingOpprettetHendelse): KnyttTilSakResultat =
        if (this.sakId == behandlingOpprettetHendelse.sakId) {
            behandlinger.add(
                Behandling(
                    behandlingId = behandlingOpprettetHendelse.behandlingId,
                    utløstAv = behandlingOpprettetHendelse.type,
                    opprettet = behandlingOpprettetHendelse.opprettet,
                    hendelse = behandlingOpprettetHendelse,
                ),
            )
            KnyttTilSakResultat.KnyttetTilSak(this)
        } else {
            KnyttTilSakResultat.IkkeKnyttetTilSak(this.sakId)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sak) return false

        if (sakId != other.sakId) return false
        if (!opprettet.isEqual(other.opprettet)) return false
        if (this.behandlinger().sortedBy { it.behandlingId } !=
            other
                .behandlinger()
                .sortedBy { it.behandlingId }
        ) {
            return false
        }
        return true
    }
}
