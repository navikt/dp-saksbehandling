package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.Hendelse
import java.util.UUID

class SøknadHendelse(private val søknadUUID: UUID, ident: String) : Hendelse(ident)
