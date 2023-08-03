package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.hendelser.Hendelse

internal fun Hendelse.toHendelseDTO() = toSpesifikkKontekst().toMap()
internal fun Collection<Hendelse>.toHendelserDTO() = this.map { it.toHendelseDTO() }
