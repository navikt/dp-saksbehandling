package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

class RapporteringsHendelse(
    private val ident: String,
    internal val rapporteringsId: UUID,
    private val rapporteringsdager: List<Rapporteringsdag>
) :
    Hendelse(ident)
