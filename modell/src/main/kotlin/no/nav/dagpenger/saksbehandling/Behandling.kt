package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import java.time.ZonedDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val person: Person,
    val opprettet: ZonedDateTime,
) {
    companion object {
        fun rehydrer(
            behandlingId: UUID,
            person: Person,
            opprettet: ZonedDateTime,
        ): Behandling {
            return Behandling(
                behandlingId = behandlingId,
                person = person,
                opprettet = opprettet,
            )
        }
    }

    fun håndter(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
//        this.oppgaver.single().oppgaveKlarTilBehandling(forslagTilVedtakHendelse)
    }

    fun håndter(vedtakFattetHendelse: VedtakFattetHendelse) {
//        this.oppgaver.single().ferdigstill(vedtakFattetHendelse)
    }

    fun håndter(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
//        this.oppgaver.add(
//            Oppgave(
//                oppgaveId = UUIDv7.ny(),
//                emneknagger = setOf("Søknadsbehandling"),
//                opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
//                tilstand = Oppgave.Opprettet,
//                ident = person.ident,
//                behandlingId = behandlingId,
//                behandling = this,
//            ),
//        )
    }
}
