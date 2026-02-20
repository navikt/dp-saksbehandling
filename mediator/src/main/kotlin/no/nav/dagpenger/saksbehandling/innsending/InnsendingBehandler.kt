package no.nav.dagpenger.saksbehandling.innsending

import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.BehandlingstypeDTO
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse

class InnsendingBehandler(
    private val klageMediator: KlageMediator,
    private val behandlingKlient: BehandlingKlient,
) {
    fun utførAksjon(
        hendelse: FerdigstillInnsendingHendelse,
        innsending: Innsending,
    ): InnsendingFerdigstiltHendelse =
        when (hendelse.aksjon) {
            is Aksjon.Avslutt ->
                InnsendingFerdigstiltHendelse(
                    innsendingId = innsending.innsendingId,
                    aksjonType = hendelse.aksjon.type,
                    opprettetBehandlingId = null,
                    utførtAv = hendelse.utførtAv,
                )

            is Aksjon.OpprettKlage ->
                opprettKlage(
                    hendelse = hendelse,
                    innsending = innsending,
                )

            is Aksjon.OpprettManuellBehandling ->
                opprettBehandling(
                    hendelse = hendelse,
                    innsending = innsending,
                )

            is Aksjon.OpprettRevurderingBehandling ->
                opprettBehandling(
                    hendelse = hendelse,
                    innsending = innsending,
                )
        }

    private fun opprettBehandling(
        hendelse: FerdigstillInnsendingHendelse,
        innsending: Innsending,
    ): InnsendingFerdigstiltHendelse {
        val vurdering =
            requireNotNull(innsending.vurdering()) { "Vurdering av innsending må være satt ved opprettelse av behandling" }

        val saksbehandlerToken =
            when (val aksjon = hendelse.aksjon) {
                is Aksjon.OpprettManuellBehandling -> aksjon.saksbehandlerToken
                is Aksjon.OpprettRevurderingBehandling -> aksjon.saksbehandlerToken
                else -> throw IllegalArgumentException("Ugyldig aksjon for opprettBehandling: $aksjon")
            }

        behandlingKlient
            .opprettBehandling(
                personIdent = innsending.person.ident,
                saksbehandlerToken = saksbehandlerToken,
                behandlingstype = hendelse.aksjon.tilBehandlingstype(),
                hendelseDato = innsending.mottatt.toLocalDate(),
                hendelseId = innsending.innsendingId.toString(),
                begrunnelse = vurdering,
            ).let { result ->
                return InnsendingFerdigstiltHendelse(
                    innsendingId = innsending.innsendingId,
                    aksjonType = hendelse.aksjon.type,
                    opprettetBehandlingId = result.getOrThrow(),
                    utførtAv = hendelse.utførtAv,
                )
            }
    }

    private fun opprettKlage(
        hendelse: FerdigstillInnsendingHendelse,
        innsending: Innsending,
    ): InnsendingFerdigstiltHendelse {
        val klageOppgave =
            klageMediator.opprettKlage(
                klageMottattHendelse =
                    KlageMottattHendelse(
                        ident = innsending.person.ident,
                        opprettet = innsending.mottatt,
                        journalpostId = innsending.journalpostId,
                        sakId = (hendelse.aksjon as Aksjon.OpprettKlage).valgtSakId,
                        utførtAv = hendelse.utførtAv,
                    ),
            )
        return InnsendingFerdigstiltHendelse(
            innsendingId = innsending.innsendingId,
            aksjonType = hendelse.aksjon.type,
            opprettetBehandlingId = klageOppgave.behandling.behandlingId,
            utførtAv = hendelse.utførtAv,
        )
    }
}

private fun Aksjon.tilBehandlingstype(): BehandlingstypeDTO =
    when (this) {
        is Aksjon.OpprettManuellBehandling -> BehandlingstypeDTO.MANUELL
        is Aksjon.OpprettRevurderingBehandling -> BehandlingstypeDTO.REVURDERING
        else -> throw IllegalArgumentException("Ugyldig aksjon for behandlingstype: $this")
    }
