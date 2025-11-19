package no.nav.dagpenger.saksbehandling.innsending

import PersonMediator
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.innsending.InnsendingRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

sealed class HåndterInnsendingResultat {
    data class HåndtertInnsending(val sakId: UUID) : HåndterInnsendingResultat()

    object UhåndtertInnsending : HåndterInnsendingResultat()
}

class InnsendingMediator(
    private val sakMediator: SakMediator,
    private val oppgaveMediator: OppgaveMediator,
    private val personMediator: PersonMediator,
    private val innsendingRepository: InnsendingRepository,
    private val innsendingBehandler: InnsendingBehandler,
) {
    fun taImotInnsending(hendelse: InnsendingMottattHendelse): HåndterInnsendingResultat {
        val skalEttersendingTilSøknadVarsles =
            hendelse.kategori == Kategori.ETTERSENDING && hendelse.søknadId != null &&
                oppgaveMediator.skalEttersendingTilSøknadVarsles(
                    søknadId = hendelse.søknadId!!,
                    ident = hendelse.ident,
                )

        val sisteSakId = sakMediator.finnSisteSakId(hendelse.ident)

        if (skalEttersendingTilSøknadVarsles || sisteSakId != null) {
            val person = personMediator.finnEllerOpprettPerson(hendelse.ident)
            val innsending =
                Innsending.opprett(hendelse = hendelse) { ident -> person }
            innsendingRepository.lagre(innsending)
            val behandling =
                Behandling(
                    behandlingId = innsending.innsendingId,
                    opprettet = innsending.mottatt,
                    hendelse = hendelse,
                    utløstAv = UtløstAvType.INNSENDING,
                )

            if (skalEttersendingTilSøknadVarsles) {
                val sakId: UUID = sakMediator.knyttEttersendingTilSammeSakSomSøknad(behandling, hendelse.søknadId!!)
                oppgaveMediator.lagOppgaveForInnsendingBehandling(
                    innsendingMottattHendelse = hendelse,
                    behandling = behandling,
                    person = person,
                )
            } else if (sisteSakId != null) {
                sakMediator.knyttBehandlingTilSak(behandling, sisteSakId)
                oppgaveMediator.lagOppgaveForInnsendingBehandling(
                    innsendingMottattHendelse = hendelse,
                    behandling = behandling,
                    person = person,
                )
            }
        }
        return when (sisteSakId) {
            null -> HåndterInnsendingResultat.UhåndtertInnsending
            else -> HåndterInnsendingResultat.HåndtertInnsending(sisteSakId)
        }
    }

    fun ferdigstill(hendelse: FerdigstillInnsendingHendelse) {
        // innsendingBehandler.utførAksjon()
        TODO()
    }

    fun avbrytInnsending(hendelse: BehandlingOpprettetForSøknadHendelse) {
//        oppgaveMediator.finnOppgaverFor(hendelse.ident)
        TODO()
    }

    fun tildel(tildelHendelse: TildelHendelse) {
        oppgaveMediator.hentOppgaveHvisTilgang(tildelHendelse.innsendingId, tildelHendelse.utførtAv)
    }
}
