package no.nav.dagpenger.saksbehandling.henvendelse

import PersonMediator
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.db.henvendelse.HenvendelseRepository
import no.nav.dagpenger.saksbehandling.hendelser.Aksjon
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillHenvendelseHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

sealed class HåndterHenvendelseResultat {
    data class HåndtertHenvendelse(val sakId: UUID) : HåndterHenvendelseResultat()

    object UhåndtertHenvendelse : HåndterHenvendelseResultat()
}

class HenvendelseMediator(
    private val sakMediator: SakMediator,
    private val oppgaveMediator: OppgaveMediator,
    private val personMediator: PersonMediator,
    private val henvendelseRepository: HenvendelseRepository,
    private val henvendelseBehandler: HenvendelseBehandler,
) {
    fun taImotHenvendelse(hendelse: HenvendelseMottattHendelse): HåndterHenvendelseResultat {
        val skalEttersendingTilSøknadVarsles =
            hendelse.kategori == Kategori.ETTERSENDING && hendelse.søknadId != null &&
                oppgaveMediator.skalEttersendingTilSøknadVarsles(
                    søknadId = hendelse.søknadId!!,
                    ident = hendelse.ident,
                )

        val sisteSakId = sakMediator.finnSisteSakId(hendelse.ident)

        if (skalEttersendingTilSøknadVarsles || sisteSakId != null) {
            val henvendelse =
                Henvendelse.opprett(hendelse = hendelse) { ident ->
                    personMediator.finnEllerOpprettPerson(ident)
                }
            henvendelseRepository.lagre(henvendelse = henvendelse)
        }
        return when (sisteSakId) {
            null -> HåndterHenvendelseResultat.UhåndtertHenvendelse
            else -> HåndterHenvendelseResultat.HåndtertHenvendelse(sisteSakId)
        }
    }

    fun ferdigstill(hendelse: FerdigstillHenvendelseHendelse) {
        val henvendelse = henvendelseRepository.hent(hendelse.henvendelseId)
        val henvendelseBehandlet: HenvendelseFerdigstiltHendelse =
            henvendelseBehandler.utførAksjon(hendelse, henvendelse)
        henvendelse.ferdigstill(henvendelseBehandlet)
        henvendelseRepository.lagre(henvendelse = henvendelse)
    }
}

interface HenvendelseBehandler {
    fun utførAksjon(
        hendelse: FerdigstillHenvendelseHendelse,
        henvendelse: Henvendelse,
    ): HenvendelseFerdigstiltHendelse
}

class HenvendelseBehandlerImpl(
    private val klageMediator: KlageMediator,
    private val behandlingKlient: BehandlingKlient,
) : HenvendelseBehandler {
    override fun utførAksjon(
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
