package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import java.time.LocalDateTime
import java.util.UUID

sealed class KnyttTilSakResultat {
    data class KnyttetTilSak(val sak: Sak) : KnyttTilSakResultat()

    data class IkkeKnyttetTilSak(val sakIder: Set<UUID>) : KnyttTilSakResultat() {
        constructor(vararg sakId: UUID) : this(sakId.toSet())
    }

    data class KnyttetTilFlereSaker(val sakIder: Set<UUID>) : KnyttTilSakResultat() {
        constructor(vararg sakId: UUID) : this(sakId.toSet())
    }
}

data class Sak(
    val sakId: UUID = UUIDv7.ny(),
    val søknadId: UUID,
    val opprettet: LocalDateTime,
    private val behandlinger: MutableSet<Behandling> = mutableSetOf(),
) {
    fun behandlinger(): List<Behandling> = behandlinger.toList()

    fun leggTilBehandling(behandling: Behandling) = behandlinger.add(behandling)

    private fun basertPåBehandlingErKnyttetTilSak(basertPåBehandlingId: UUID?): Boolean {
        return this.behandlinger.map { it.behandlingId }
            .contains(basertPåBehandlingId)
    }

    fun knyttTilSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse): KnyttTilSakResultat {
        return when (
            this.sakId == søknadsbehandlingOpprettetHendelse.behandlingskjedeId ||
                this.basertPåBehandlingErKnyttetTilSak(
                    søknadsbehandlingOpprettetHendelse.basertPåBehandling,
                )
        ) {
            true -> {
                behandlinger.add(
                    Behandling(
                        behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
                        utløstAv = UtløstAvType.SØKNAD,
                        opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
                        hendelse = søknadsbehandlingOpprettetHendelse,
                    ),
                )
                KnyttTilSakResultat.KnyttetTilSak(this)
            }

            else -> {
                KnyttTilSakResultat.IkkeKnyttetTilSak(this.sakId)
            }
        }
    }

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse): KnyttTilSakResultat {
        return if (this.sakId == meldekortbehandlingOpprettetHendelse.behandlingskjedeId) {
            behandlinger.add(
                Behandling(
                    behandlingId = meldekortbehandlingOpprettetHendelse.behandlingId,
                    utløstAv = UtløstAvType.MELDEKORT,
                    opprettet = meldekortbehandlingOpprettetHendelse.opprettet,
                    hendelse = meldekortbehandlingOpprettetHendelse,
                ),
            )
            KnyttTilSakResultat.KnyttetTilSak(this)
        } else {
            KnyttTilSakResultat.IkkeKnyttetTilSak(this.sakId)
        }
    }

    fun knyttTilSak(manuellBehandlingOpprettetHendelse: ManuellBehandlingOpprettetHendelse): KnyttTilSakResultat {
        return if (this.sakId == manuellBehandlingOpprettetHendelse.behandlingskjedeId) {
            behandlinger.add(
                Behandling(
                    behandlingId = manuellBehandlingOpprettetHendelse.behandlingId,
                    utløstAv = UtløstAvType.MANUELL,
                    opprettet = manuellBehandlingOpprettetHendelse.opprettet,
                    hendelse = manuellBehandlingOpprettetHendelse,
                ),
            )
            KnyttTilSakResultat.KnyttetTilSak(this)
        } else {
            KnyttTilSakResultat.IkkeKnyttetTilSak(this.sakId)
        }
    }

    fun knyttTilSak(behandlingOpprettetHendelse: BehandlingOpprettetHendelse): KnyttTilSakResultat {
        return if (this.sakId == behandlingOpprettetHendelse.sakId) {
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
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sak) return false

        if (sakId != other.sakId) return false
        if (søknadId != other.søknadId) return false
        if (!opprettet.isEqual(other.opprettet)) return false
        if (this.behandlinger().sortedBy { it.behandlingId } !=
            other.behandlinger()
                .sortedBy { it.behandlingId }
        ) {
            return false
        }
        return true
    }
}
