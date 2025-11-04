package no.nav.dagpenger.saksbehandling.henvendelse

import PersonMediator
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.db.henvendelse.HenvendelseRepository
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettKlageHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID
import no.nav.dagpenger.saksbehandling.hendelser.Aksjon
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillHenvendelseHendelse

sealed class HåndterHenvendelseResultat {
    data class HåndtertHenvendelse(val sakId: UUID) : HåndterHenvendelseResultat()

    object UhåndtertHenvendelse : HåndterHenvendelseResultat()
}

class HenvendelseMediator(
    private val sakMediator: SakMediator,
    private val oppgaveMediator: OppgaveMediator,
    private val personMediator: PersonMediator,
    private val klageMediator: KlageMediator,
    private val henvendelseRepository: HenvendelseRepository,
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
        val henvendelseBehandlet = when (hendelse.aksjon) {
            Aksjon.Avslutt -> TODO()
            Aksjon.OpprettKlage -> TODO()
            Aksjon.OpprettManuellBehandling -> TODO()
        }
        henvendelse.ferdigstill(henvendelseBehandlet)
    }

    fun ferdigstill(hendelse: OpprettKlageHendelse) {
        val henvendelse = henvendelseRepository.hent(hendelse.henvendelseId)
        val oppgave =
            klageMediator.opprettKlage(
                klageMottattHendelse =
                    KlageMottattHendelse(
                        ident = hendelse.ident,
                        opprettet = hendelse.mottatt,
                        journalpostId = hendelse.journalpostId,
                        sakId = hendelse.sakId,
                        utførtAv = hendelse.utførtAv,
                    ),
            )
        henvendelse.ferdigstill(
            KlageOpprettetHendelse(
                behandlingId = oppgave.behandling.behandlingId,
                ident = hendelse.ident,
                mottatt = hendelse.mottatt,
                journalpostId = hendelse.journalpostId,
                sakId = hendelse.sakId,
                utførtAv = hendelse.utførtAv,
            ),
        )
    }
}
