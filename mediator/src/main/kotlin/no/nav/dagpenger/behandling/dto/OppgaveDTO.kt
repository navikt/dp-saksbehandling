package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.oppgave.Oppgave
import java.time.LocalDate
import java.util.UUID

internal data class OppgaveDTO(
    val uuid: UUID,
    val person: String,
    val saksbehandler: String?,
    val opprettet: LocalDate,
    val hendelse: List<HendelseDTO>,
    val journalposter: List<String>,
    val tilstand: String,
    val steg: List<StegDTO>,
    val muligeTilstander: List<String>,
)

internal fun Collection<Oppgave>.toOppgaverDTO() = this.map { it.toOppgaveDTO() }

internal fun Oppgave.toOppgaveDTO(): OppgaveDTO {
    return OppgaveDTO(
        uuid = this.uuid,
        person = this.person.ident,
        saksbehandler = null,
        opprettet = this.opprettet.toLocalDate(),
        hendelse = this.behandler.toHendelserDTO(),
        journalposter = this.behandler.toHendelserDTO().mapNotNull { it["journalpostId"] },
        tilstand = this.tilstand() ?: "🚨 Mangler tilstand 😱", // TODO: Finne ut hvorfor noen oppgaver mangler tilstand
        muligeTilstander = this.muligeTilstander(),
        steg = this.alleSteg().toStegDTO(),
    )
}
