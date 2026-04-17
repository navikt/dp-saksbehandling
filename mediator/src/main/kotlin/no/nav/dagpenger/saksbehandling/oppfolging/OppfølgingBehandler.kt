package no.nav.dagpenger.saksbehandling.oppfolging

import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.BehandlingstypeDTO
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppfølgingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse

class OppfølgingBehandler(
    private val klageMediator: KlageMediator,
    private val behandlingKlient: BehandlingKlient,
) {
    fun utførAksjon(
        oppfølging: Oppfølging,
        hendelse: FerdigstillOppfølgingHendelse,
        oppfølgingMediator: OppfølgingMediator,
    ): OppfølgingFerdigstiltHendelse =
        when (hendelse.aksjon) {
            is OppfølgingAksjon.Avslutt ->
                OppfølgingFerdigstiltHendelse(
                    oppfølgingId = oppfølging.id,
                    aksjonType = hendelse.aksjon.type,
                    opprettetBehandlingId = null,
                    utførtAv = hendelse.utførtAv,
                )

            is OppfølgingAksjon.OpprettKlage ->
                opprettKlage(
                    oppfølging = oppfølging,
                    hendelse = hendelse,
                )

            is OppfølgingAksjon.OpprettManuellBehandling ->
                opprettBehandling(
                    oppfølging = oppfølging,
                    hendelse = hendelse,
                )

            is OppfølgingAksjon.OpprettRevurderingBehandling ->
                opprettBehandling(
                    oppfølging = oppfølging,
                    hendelse = hendelse,
                )

            is OppfølgingAksjon.OpprettOppfølging ->
                opprettNyOppfølging(
                    oppfølging = oppfølging,
                    hendelse = hendelse,
                    oppfølgingMediator = oppfølgingMediator,
                )
        }

    private fun opprettBehandling(
        oppfølging: Oppfølging,
        hendelse: FerdigstillOppfølgingHendelse,
    ): OppfølgingFerdigstiltHendelse {
        val (saksbehandlerToken, behandlingstype) =
            when (val aksjon = hendelse.aksjon) {
                is OppfølgingAksjon.OpprettManuellBehandling ->
                    aksjon.saksbehandlerToken to BehandlingstypeDTO.MANUELL
                is OppfølgingAksjon.OpprettRevurderingBehandling ->
                    aksjon.saksbehandlerToken to BehandlingstypeDTO.REVURDERING
                else -> throw IllegalArgumentException("Ugyldig aksjon for opprettBehandling: $aksjon")
            }

        behandlingKlient
            .opprettBehandling(
                personIdent = oppfølging.person.ident,
                saksbehandlerToken = saksbehandlerToken,
                behandlingstype = behandlingstype,
                hendelseDato = oppfølging.opprettet.toLocalDate(),
                hendelseId = oppfølging.id.toString(),
                begrunnelse = oppfølging.vurdering() ?: "Opprettet fra oppfølging",
            ).let { result ->
                return OppfølgingFerdigstiltHendelse(
                    oppfølgingId = oppfølging.id,
                    aksjonType = hendelse.aksjon.type,
                    opprettetBehandlingId = result.getOrThrow(),
                    utførtAv = hendelse.utførtAv,
                )
            }
    }

    private fun opprettKlage(
        oppfølging: Oppfølging,
        hendelse: FerdigstillOppfølgingHendelse,
    ): OppfølgingFerdigstiltHendelse {
        val aksjon = hendelse.aksjon as OppfølgingAksjon.OpprettKlage

        val klageOppgave =
            klageMediator.opprettKlage(
                klageMottattHendelse =
                    KlageMottattHendelse(
                        ident = oppfølging.person.ident,
                        opprettet = oppfølging.opprettet,
                        journalpostId = null,
                        sakId = aksjon.valgtSakId,
                        utførtAv = hendelse.utførtAv,
                    ),
            )

        return OppfølgingFerdigstiltHendelse(
            oppfølgingId = oppfølging.id,
            aksjonType = hendelse.aksjon.type,
            opprettetBehandlingId = klageOppgave.behandling.behandlingId,
            utførtAv = hendelse.utførtAv,
        )
    }

    private fun opprettNyOppfølging(
        oppfølging: Oppfølging,
        hendelse: FerdigstillOppfølgingHendelse,
        oppfølgingMediator: OppfølgingMediator,
    ): OppfølgingFerdigstiltHendelse {
        val aksjon = hendelse.aksjon as OppfølgingAksjon.OpprettOppfølging

        val nyOppgaveHendelse =
            OpprettOppfølgingHendelse(
                ident = oppfølging.person.ident,
                aarsak = aksjon.aarsak,
                tittel = aksjon.tittel,
                beskrivelse = aksjon.beskrivelse,
                frist = aksjon.frist,
                utførtAv = hendelse.utførtAv,
            )

        val opprettet = oppfølgingMediator.taImot(nyOppgaveHendelse)

        return OppfølgingFerdigstiltHendelse(
            oppfølgingId = oppfølging.id,
            aksjonType = aksjon.type,
            opprettetBehandlingId = opprettet.oppfølgingId,
            opprettetOppgaveId = opprettet.oppgaveId,
            beholdOppgaven = aksjon.beholdOppgaven,
            utførtAv = hendelse.utførtAv,
        )
    }
}
