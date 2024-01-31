package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.api.models.OppgaveDTO
import no.nav.dagpenger.behandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.behandling.oppgave.Oppgave
import java.util.UUID

internal fun Collection<Oppgave>.toOppgaverDTO() = this.map { it.toOppgaveDTO() }

internal fun Oppgave.toOppgaveDTO(): OppgaveDTO {
    return OppgaveDTO(
        uuid = this.uuid,
        person = this.person.ident,
        saksbehandler = null,
        opprettet = this.opprettet.toLocalDate(),
        hendelse = this.behandler.toHendelserDTO().map { it.toMap() },
        journalposter = this.behandler.toHendelserDTO().mapNotNull { it.kontekstMap["journalpostId"] },
        tilstand = OppgaveTilstandDTO.valueOf(this.tilstand.toString()),
        steg = this.alleSteg().toStegDTO(),
        emneknagger = this.emneknagger.toList(),
    )
}

data class OppgaveIdWrapper(val oppgaveId: UUID)
