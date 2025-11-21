package no.nav.dagpenger.saksbehandling.innsending

import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
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
    ): InnsendingFerdigstiltHendelse {
        return when (hendelse.aksjon) {
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
                opprettManuellBehandling(
                    hendelse = hendelse,
                    innsending = innsending,
                )
        }
    }

    private fun opprettManuellBehandling(
        hendelse: FerdigstillInnsendingHendelse,
        innsending: Innsending,
    ): InnsendingFerdigstiltHendelse {
        behandlingKlient.opprettManuellBehandling(
            personIdent = innsending.person.ident,
            saksbehandlerToken = (hendelse.aksjon as Aksjon.OpprettManuellBehandling).saksbehandlerToken,
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
