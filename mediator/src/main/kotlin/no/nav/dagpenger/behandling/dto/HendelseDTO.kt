package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.hendelser.PersonHendelse

internal fun PersonHendelse.toHendelseDTO() = toSpesifikkKontekst()
internal fun Collection<PersonHendelse>.toHendelserDTO() = this.map { it.toHendelseDTO() }
