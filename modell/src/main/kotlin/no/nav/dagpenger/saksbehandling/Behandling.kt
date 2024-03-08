package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import java.util.UUID

// @todo: Tydeliggjøre forskjellen på oppgave og behandling? Trenger vi behandling? Har behandling tilstand?
data class Behandling(
    val behandlingId: UUID,
    val person: Person,
    val oppgaver: MutableList<Oppgave> = mutableListOf(),
) {
    fun håndter(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        this.oppgaver.single {
            it.tilstand == Oppgave.Tilstand.Type.OPPRETTET && it.emneknagger.contains("Søknadsbehandling")
        }.håndter(forslagTilVedtakHendelse)

    }

    fun håndter(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        this.oppgaver.add(
            Oppgave(
                oppgaveId = UUIDv7.ny(),
                emneknagger = setOf("Søknadsbehandling"),
                opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
                tilstand = Oppgave.Tilstand.Type.OPPRETTET
            )
        )
    }
}
