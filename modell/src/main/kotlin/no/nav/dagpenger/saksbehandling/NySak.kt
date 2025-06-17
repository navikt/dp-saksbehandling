package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import java.time.LocalDateTime
import java.util.UUID

data class NySak(
    val sakId: UUID = UUIDv7.ny(),
    val søknadId: UUID,
    val opprettet: LocalDateTime,
    private val behandlinger: MutableSet<NyBehandling> = mutableSetOf(),
) {
    fun behandlinger(): List<NyBehandling> = behandlinger.toList()

    fun leggTilBehandling(behandling: NyBehandling) = behandlinger.add(behandling)

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse) {
        if (this.behandlinger.map { it.behandlingId }
                .containsAll(meldekortbehandlingOpprettetHendelse.basertPåBehandlinger)
        ) {
            behandlinger.add(
                NyBehandling(
                    behandlingId = meldekortbehandlingOpprettetHendelse.behandlingId,
                    behandlingType = BehandlingType.MELDEKORT,
                    opprettet = meldekortbehandlingOpprettetHendelse.opprettet,
                ),
            )
        }
    }

    fun knyttTilSak(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        behandlinger.add(
            NyBehandling(
                behandlingId = behandlingOpprettetHendelse.behandlingId,
                behandlingType = behandlingOpprettetHendelse.type,
                opprettet = behandlingOpprettetHendelse.opprettet,
            ),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NySak) return false

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

data class NyBehandling(
    val behandlingId: UUID,
    val behandlingType: BehandlingType,
    val opprettet: LocalDateTime,
    val oppgaveId: UUID? = null,
)
