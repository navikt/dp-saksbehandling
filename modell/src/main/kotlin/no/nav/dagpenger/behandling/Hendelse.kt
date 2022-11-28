package no.nav.dagpenger.behandling

import java.util.UUID

abstract class Hendelse(private val ident: String)

class SøknadHendelse(private val søknadUUID: UUID, ident: String) : Hendelse(ident)

internal class RapporteringHendelse(ident: String) : Hendelse(ident)

internal class KnappestøperHendelse(ident: String) : Hendelse(ident)
