package no.nav.dagpenger.behandling.modell.hendelser

import java.time.LocalDate
import java.util.UUID

data class SøknadInnsendtHendelse(
    val meldingsreferanseId: UUID = UUID.randomUUID(),
    val søknadId: UUID,
    val journalpostId: String,
    val ident: String,
    val innsendtDato: LocalDate,
)
