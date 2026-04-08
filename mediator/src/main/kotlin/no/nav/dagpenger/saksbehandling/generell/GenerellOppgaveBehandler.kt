package no.nav.dagpenger.saksbehandling.generell

import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.BehandlingstypeDTO
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GenerellOppgaveFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse

class GenerellOppgaveBehandler(
    private val klageMediator: KlageMediator,
    private val behandlingKlient: BehandlingKlient,
) {
    fun utførAksjon(
        generellOppgave: GenerellOppgave,
        hendelse: FerdigstillGenerellOppgaveHendelse,
    ): GenerellOppgaveFerdigstiltHendelse =
        when (hendelse.aksjon) {
            is GenerellOppgaveAksjon.Avslutt ->
                GenerellOppgaveFerdigstiltHendelse(
                    generellOppgaveId = generellOppgave.id,
                    aksjonType = hendelse.aksjon.type,
                    opprettetBehandlingId = null,
                    utførtAv = hendelse.utførtAv,
                )

            is GenerellOppgaveAksjon.OpprettKlage ->
                opprettKlage(
                    generellOppgave = generellOppgave,
                    hendelse = hendelse,
                )

            is GenerellOppgaveAksjon.OpprettManuellBehandling ->
                opprettBehandling(
                    generellOppgave = generellOppgave,
                    hendelse = hendelse,
                )
        }

    private fun opprettBehandling(
        generellOppgave: GenerellOppgave,
        hendelse: FerdigstillGenerellOppgaveHendelse,
    ): GenerellOppgaveFerdigstiltHendelse {
        val aksjon = hendelse.aksjon as GenerellOppgaveAksjon.OpprettManuellBehandling

        behandlingKlient
            .opprettBehandling(
                personIdent = generellOppgave.person.ident,
                saksbehandlerToken = aksjon.saksbehandlerToken,
                behandlingstype = BehandlingstypeDTO.MANUELL,
                hendelseDato = generellOppgave.opprettet.toLocalDate(),
                hendelseId = generellOppgave.id.toString(),
                begrunnelse = generellOppgave.vurdering() ?: "Opprettet fra generell oppgave",
            ).let { result ->
                return GenerellOppgaveFerdigstiltHendelse(
                    generellOppgaveId = generellOppgave.id,
                    aksjonType = hendelse.aksjon.type,
                    opprettetBehandlingId = result.getOrThrow(),
                    utførtAv = hendelse.utførtAv,
                )
            }
    }

    private fun opprettKlage(
        generellOppgave: GenerellOppgave,
        hendelse: FerdigstillGenerellOppgaveHendelse,
    ): GenerellOppgaveFerdigstiltHendelse {
        val aksjon = hendelse.aksjon as GenerellOppgaveAksjon.OpprettKlage

        val klageOppgave =
            klageMediator.opprettKlage(
                klageMottattHendelse =
                    KlageMottattHendelse(
                        ident = generellOppgave.person.ident,
                        opprettet = generellOppgave.opprettet,
                        journalpostId = null,
                        sakId = aksjon.valgtSakId,
                        utførtAv = hendelse.utførtAv,
                    ),
            )

        return GenerellOppgaveFerdigstiltHendelse(
            generellOppgaveId = generellOppgave.id,
            aksjonType = hendelse.aksjon.type,
            opprettetBehandlingId = klageOppgave.behandling.behandlingId,
            utførtAv = hendelse.utførtAv,
        )
    }
}
