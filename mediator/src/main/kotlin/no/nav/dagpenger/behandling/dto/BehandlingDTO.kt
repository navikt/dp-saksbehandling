package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.Behandling
import java.time.LocalDate
import java.util.UUID

internal data class BehandlingDTO(
    val uuid: UUID,
    val person: String,
    val saksbehandler: String?,
    val opprettet: LocalDate,
    val hendelse: List<HendelseDTO>,
    val tilstand: String,
    val steg: List<StegDTO>,
)

internal fun Collection<Behandling>.toBehandlingerDTO() = this.map { it.toBehandlingDTO() }

internal fun Behandling.toBehandlingDTO(): BehandlingDTO {
    return BehandlingDTO(
        uuid = this.uuid,
        person = this.person.ident,
        saksbehandler = null,
        opprettet = this.opprettet.toLocalDate(),
        hendelse = emptyList(),
        tilstand = if (this.erBehandlet()) {
            "FerdigBehandlet"
        } else {
            "TilBehandling"
        },
        steg = this.alleSteg().toStegDTO(),
    )
}
