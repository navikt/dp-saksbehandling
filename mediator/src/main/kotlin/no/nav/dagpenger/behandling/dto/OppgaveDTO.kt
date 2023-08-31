package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.api.models.OppgaveDTO
import no.nav.dagpenger.behandling.oppgave.Oppgave

internal fun Collection<Oppgave>.toOppgaverDTO() = this.map { it.toOppgaveDTO() }

internal fun Oppgave.toOppgaveDTO(): OppgaveDTO {
    return OppgaveDTO(
        uuid = this.uuid,
        person = this.person.ident,
        saksbehandler = null,
        opprettet = this.opprettet.toLocalDate(),
        hendelse = this.behandler.toHendelserDTO().map { it.toMap() },
        journalposter = this.behandler.toHendelserDTO().mapNotNull { it.kontekstMap["journalpostId"] },
        tilstand = this.tilstand
            ?: throw Exception("🚨 Mangler tilstand 😱"), // TODO: Finne ut hvorfor noen oppgaver mangler tilstand
        muligeTilstander = this.muligeTilstander(),
        steg = this.alleSteg().toStegDTO(),
    )
}
