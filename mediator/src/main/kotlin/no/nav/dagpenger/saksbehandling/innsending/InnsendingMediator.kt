package no.nav.dagpenger.saksbehandling.innsending

import PersonMediator
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.innsending.InnsendingRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.tilgangsstyring.SaksbehandlerErIkkeEier
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
            val innsending =
                Innsending.opprett(hendelse = hendelse) { ident ->
                    personMediator.finnEllerOpprettPerson(ident)
                }
            val oppgave =
                oppgaveMediator.opprettOppgaveForBehandling(
                    BehandlingOpprettetHendelse(
                        behandlingId = innsending.innsendingId,
                        ident = innsending.person.ident,
                        // todo nullpointer her hvis sisteSakId er null
                        sakId = sisteSakId!!,
                        opprettet = hendelse.registrertTidspunkt,
                        type = UtløstAvType.INNSENDING,
                        utførtAv = hendelse.utførtAv,
                    ),
                )
            innsendingRepository.lagre(innsending)
        }

        return when (sisteSakId) {
            null -> HåndterInnsendingResultat.UhåndtertInnsending
            else -> HåndterInnsendingResultat.HåndtertInnsending(sisteSakId)
        }
    }

    fun ferdigstill(hendelse: FerdigstillInnsendingHendelse) {
        val innsending = hentInnsending(innsendingId = hendelse.innsendingId, behandler = hendelse.utførtAv)
        requireEierskap(innsending = innsending, behandler = hendelse.utførtAv)
        val innsendingFerdigstiltHendelse = innsendingBehandler.utførAksjon(hendelse, innsending)
        innsending.ferdigstill(innsendingFerdigstiltHendelse)
        innsendingRepository.lagre(innsending)
        oppgaveMediator.ferdigstillOppgave(innsendingFerdigstiltHendelse)
    }

    fun avbrytInnsending(hendelse: BehandlingOpprettetForSøknadHendelse) {
        val innsendinger = innsendingRepository.finnInnsendingerForPerson(ident = hendelse.ident)
        innsendinger.singleOrNull {
            it.tilstand().type == KLAR_TIL_BEHANDLING && it.gjelderSøknadMedId(søknadId = hendelse.søknadId)
        }?.let { innsending ->
            innsending.avbryt(hendelse)
            innsendingRepository.lagre(innsending)
        }
    }

    fun tildel(tildelHendelse: TildelHendelse) {
        hentInnsending(
            innsendingId = tildelHendelse.innsendingId,
            behandler = tildelHendelse.utførtAv,
        ).let { innsending ->
            innsending.tildel(tildelHendelse)
            innsendingRepository.lagre(innsending)
        }
    }

    fun hentInnsending(
        innsendingId: UUID,
        behandler: Behandler,
    ): Innsending {
        return innsendingRepository.hent(innsendingId).also { innsending ->
            innsending.harTilgang(behandler)
        }
    }

    private fun requireEierskap(
        innsending: Innsending,
        behandler: Behandler,
    ) {
        when (behandler) {
            is Applikasjon -> {}
            is Saksbehandler -> {
                if (innsending.behandlerIdent() != behandler.navIdent) {
                    throw SaksbehandlerErIkkeEier(
                        "Saksbehandler ${behandler.navIdent} eier ikke " +
                            "innsendingen ${innsending.innsendingId}",
                    )
                }
            }
        }
    }
}
