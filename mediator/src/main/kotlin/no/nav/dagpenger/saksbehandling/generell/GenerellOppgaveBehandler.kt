package no.nav.dagpenger.saksbehandling.generell

import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.BehandlingstypeDTO
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GenerellOppgaveFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse

class GenerellOppgaveBehandler(
    private val klageMediator: KlageMediator,
    private val behandlingKlient: BehandlingKlient,
) {
    fun utførAksjon(
        generellOppgave: GenerellOppgave,
        hendelse: FerdigstillGenerellOppgaveHendelse,
        generellOppgaveMediator: GenerellOppgaveMediator,
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

            is GenerellOppgaveAksjon.OpprettRevurderingBehandling ->
                opprettBehandling(
                    generellOppgave = generellOppgave,
                    hendelse = hendelse,
                )

            is GenerellOppgaveAksjon.OpprettGenerellOppgave ->
                opprettNyGenerellOppgave(
                    generellOppgave = generellOppgave,
                    hendelse = hendelse,
                    generellOppgaveMediator = generellOppgaveMediator,
                )
        }

    private fun opprettBehandling(
        generellOppgave: GenerellOppgave,
        hendelse: FerdigstillGenerellOppgaveHendelse,
    ): GenerellOppgaveFerdigstiltHendelse {
        val (saksbehandlerToken, behandlingstype) =
            when (val aksjon = hendelse.aksjon) {
                is GenerellOppgaveAksjon.OpprettManuellBehandling ->
                    aksjon.saksbehandlerToken to BehandlingstypeDTO.MANUELL
                is GenerellOppgaveAksjon.OpprettRevurderingBehandling ->
                    aksjon.saksbehandlerToken to BehandlingstypeDTO.REVURDERING
                else -> throw IllegalArgumentException("Ugyldig aksjon for opprettBehandling: $aksjon")
            }

        behandlingKlient
            .opprettBehandling(
                personIdent = generellOppgave.person.ident,
                saksbehandlerToken = saksbehandlerToken,
                behandlingstype = behandlingstype,
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

    private fun opprettNyGenerellOppgave(
        generellOppgave: GenerellOppgave,
        hendelse: FerdigstillGenerellOppgaveHendelse,
        generellOppgaveMediator: GenerellOppgaveMediator,
    ): GenerellOppgaveFerdigstiltHendelse {
        val aksjon = hendelse.aksjon as GenerellOppgaveAksjon.OpprettGenerellOppgave

        val nyOppgaveHendelse =
            OpprettGenerellOppgaveHendelse(
                ident = generellOppgave.person.ident,
                aarsak = aksjon.aarsak,
                tittel = aksjon.tittel,
                beskrivelse = aksjon.beskrivelse,
                frist = aksjon.frist,
                utførtAv = hendelse.utførtAv,
            )

        val opprettet = generellOppgaveMediator.taImot(nyOppgaveHendelse)

        return GenerellOppgaveFerdigstiltHendelse(
            generellOppgaveId = generellOppgave.id,
            aksjonType = aksjon.type,
            opprettetBehandlingId = opprettet.generellOppgaveId,
            opprettetOppgaveId = opprettet.oppgaveId,
            tildelSammeSaksbehandler = aksjon.tildelSammeSaksbehandler,
            utførtAv = hendelse.utførtAv,
        )
    }
}
