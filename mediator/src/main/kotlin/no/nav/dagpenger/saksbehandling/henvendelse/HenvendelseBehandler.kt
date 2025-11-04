package no.nav.dagpenger.saksbehandling.henvendelse

import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.hendelser.Aksjon
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillHenvendelseHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse

class HenvendelseBehandler(
    private val klageMediator: KlageMediator,
    private val behandlingKlient: BehandlingKlient,
) {
    fun utførAksjon(
        hendelse: FerdigstillHenvendelseHendelse,
        henvendelse: Henvendelse,
    ): HenvendelseFerdigstiltHendelse {
        return when (hendelse.aksjon) {
            Aksjon.Avslutt ->
                HenvendelseFerdigstiltHendelse(
                    henvendelseId = henvendelse.henvendelseId,
                    aksjon = hendelse.aksjon.javaClass.simpleName,
                    behandlingId = null,
                    utførtAv = hendelse.utførtAv,
                )

            is Aksjon.OpprettKlage -> opprettKlage(hendelse = hendelse, henvendelse = henvendelse)
            is Aksjon.OpprettManuellBehandling ->
                opprettManuellBehandling(
                    hendelse = hendelse,
                    henvendelse = henvendelse,
                )
        }
    }

    private fun opprettManuellBehandling(
        hendelse: FerdigstillHenvendelseHendelse,
        henvendelse: Henvendelse,
    ): HenvendelseFerdigstiltHendelse {
        behandlingKlient.opprettManuellBehandling(
            personIdent = henvendelse.person.ident,
            saksbehandlerToken = (hendelse.aksjon as Aksjon.OpprettManuellBehandling).saksbehandlerToken,
        ).let { result ->
            return HenvendelseFerdigstiltHendelse(
                henvendelseId = henvendelse.henvendelseId,
                aksjon = hendelse.aksjon.javaClass.simpleName,
                behandlingId = result.getOrThrow(),
                utførtAv = hendelse.utførtAv,
            )
        }
    }

    private fun opprettKlage(
        hendelse: FerdigstillHenvendelseHendelse,
        henvendelse: Henvendelse,
    ): HenvendelseFerdigstiltHendelse {
        val klageOppgave =
            klageMediator.opprettKlage(
                klageMottattHendelse =
                    KlageMottattHendelse(
                        ident = henvendelse.person.ident,
                        opprettet = henvendelse.mottatt,
                        journalpostId = henvendelse.journalpostId,
                        sakId = (hendelse.aksjon as Aksjon.OpprettKlage).sakId,
                        utførtAv = hendelse.utførtAv,
                    ),
            )
        return HenvendelseFerdigstiltHendelse(
            henvendelseId = henvendelse.henvendelseId,
            aksjon = hendelse.aksjon.javaClass.simpleName,
            behandlingId = klageOppgave.behandling.behandlingId,
            utførtAv = hendelse.utførtAv,
        )
    }
}
