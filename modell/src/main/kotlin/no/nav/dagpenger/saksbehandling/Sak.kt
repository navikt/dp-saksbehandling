package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import java.time.LocalDateTime
import java.util.UUID

data class Sak(
    val sakId: UUID = UUIDv7.ny(),
    val søknadId: UUID,
    val opprettet: LocalDateTime,
    private val behandlinger: MutableSet<Behandling> = mutableSetOf(),
) {
    fun behandlinger(): List<Behandling> = behandlinger.toList()

    fun leggTilBehandling(behandling: Behandling) = behandlinger.add(behandling)

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse) {
        if (this.behandlinger.map { it.behandlingId }
                .contains(meldekortbehandlingOpprettetHendelse.basertPåBehandling)
        ) {
            behandlinger.add(
                Behandling(
                    behandlingId = meldekortbehandlingOpprettetHendelse.behandlingId,
                    type = BehandlingType.MELDEKORT,
                    opprettet = meldekortbehandlingOpprettetHendelse.opprettet,
                    hendelse = meldekortbehandlingOpprettetHendelse,
                ),
            )
        }
    }

    fun knyttTilSak(manuellBehandlingOpprettetHendelse: ManuellBehandlingOpprettetHendelse) {
        val forrigeBehandling: Behandling? =
            this.behandlinger.find { it.behandlingId == manuellBehandlingOpprettetHendelse.basertPåBehandling }

        if (forrigeBehandling != null) {
            behandlinger.add(
                Behandling(
                    behandlingId = manuellBehandlingOpprettetHendelse.behandlingId,
                    type = forrigeBehandling.type,
                    opprettet = manuellBehandlingOpprettetHendelse.opprettet,
                    hendelse = manuellBehandlingOpprettetHendelse,
                ),
            )
        }
    }

    fun knyttTilSak(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        behandlinger.add(
            Behandling(
                behandlingId = behandlingOpprettetHendelse.behandlingId,
                type = behandlingOpprettetHendelse.type,
                opprettet = behandlingOpprettetHendelse.opprettet,
                hendelse = behandlingOpprettetHendelse,
            ),
        )
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
