package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.hendelser.Hendelse

/*internal data class HendelseDTO(
    val id: String,
    val type: String,
    val tilstand: String,
)*/
typealias HendelseDTO = Map<String, Any>

internal fun Hendelse.toHendelseDTO() = toSpesifikkKontekst().toMap()
internal fun Collection<Hendelse>.toHendelserDTO() = this.map { it.toHendelseDTO() }
