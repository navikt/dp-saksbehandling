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
        constructor(vararg saksid: UUID) : this(saksid.toSet())
    }

    data class KnyttetTilFlereSaker(val sakIder: Set<UUID>) : KnyttTilSakResultat() {
        constructor(vararg saksid: UUID) : this(saksid.toSet())
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

    fun knyttTilSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse): KnyttTilSakResultat {
        return when (
            this.behandlinger.map { it.behandlingId }
                .contains(søknadsbehandlingOpprettetHendelse.basertPåBehandling)
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
        return when (
            this.behandlinger.map { it.behandlingId }
                .contains(meldekortbehandlingOpprettetHendelse.basertPåBehandling)
        ) {
            true -> {
                behandlinger.add(
                    Behandling(
                        behandlingId = meldekortbehandlingOpprettetHendelse.behandlingId,
                        utløstAv = UtløstAvType.MELDEKORT,
                        opprettet = meldekortbehandlingOpprettetHendelse.opprettet,
                        hendelse = meldekortbehandlingOpprettetHendelse,
                    ),
                )
                KnyttTilSakResultat.KnyttetTilSak(this)
            }

            false -> {
                KnyttTilSakResultat.IkkeKnyttetTilSak(this.sakId)
            }
        }
    }

    fun knyttTilSak(manuellBehandlingOpprettetHendelse: ManuellBehandlingOpprettetHendelse): KnyttTilSakResultat {
        val forrigeBehandling: Behandling? =
            this.behandlinger.find { it.behandlingId == manuellBehandlingOpprettetHendelse.basertPåBehandling }

        return if (forrigeBehandling != null) {
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
        return when (this.sakId == behandlingOpprettetHendelse.sakId) {
            true -> {
                behandlinger.add(
                    Behandling(
                        behandlingId = behandlingOpprettetHendelse.behandlingId,
                        utløstAv = behandlingOpprettetHendelse.type,
                        opprettet = behandlingOpprettetHendelse.opprettet,
                        hendelse = behandlingOpprettetHendelse,
                    ),
                )
                KnyttTilSakResultat.KnyttetTilSak(this)
            }

            else -> {
                KnyttTilSakResultat.IkkeKnyttetTilSak(this.sakId)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sak) return false

        if (sakId != other.sakId) return false
        if (søknadId != other.søknadId) return false
        if (opprettet != other.opprettet) return false
        if (this.behandlinger().sortedBy { it.behandlingId } !=
            other.behandlinger()
                .sortedBy { it.behandlingId }
        ) {
            return false
        }
        return true
    }
}
