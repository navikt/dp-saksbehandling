package no.nav.dagpenger.saksbehandling.innsending

import PersonMediator
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.innsending.InnsendingRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

sealed class HåndterInnsendingResultat {
    data class HåndtertInnsending(
        val sakId: UUID,
    ) : HåndterInnsendingResultat()

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
        oppgaveMediator.taImotEttersending(hendelse)

        val sisteSakId = sakMediator.finnSisteSakId(hendelse.ident)

        if (sisteSakId != null) {
            if (hendelse.erEttersendingMedSøknadId()) {
                taImotEttersendingTilSøknad(hendelse)
            } else {
                taImotInnsendingPåSisteSak(hendelse, sisteSakId)
            }
        }

        return when (sisteSakId) {
            null -> HåndterInnsendingResultat.UhåndtertInnsending
            else -> HåndterInnsendingResultat.HåndtertInnsending(sisteSakId)
        }
    }

    private fun søknadErFerdigBehandlet(hendelse: InnsendingMottattHendelse): Boolean =
        oppgaveMediator.oppgaveTilstandForSøknad(
            hendelse.søknadId!!,
            hendelse.ident,
        ) == Oppgave.Tilstand.Type.FERDIG_BEHANDLET

    private fun taImotEttersendingTilSøknad(hendelse: InnsendingMottattHendelse) {
        if (søknadErFerdigBehandlet(hendelse)) {
            val person = personMediator.finnEllerOpprettPerson(hendelse.ident)
            val innsending = Innsending.opprett(hendelse = hendelse) { ident -> person }
            innsendingRepository.lagre(innsending)
            val behandling =
                Behandling(
                    behandlingId = innsending.innsendingId,
                    opprettet = innsending.mottatt,
                    hendelse = hendelse,
                    utløstAv = UtløstAvType.INNSENDING,
                )
            sakMediator.knyttEttersendingTilSammeSakSomSøknad(
                behandling = behandling,
                hendelse = hendelse,
            )
            oppgaveMediator.lagOppgaveForInnsendingBehandling(hendelse, behandling, person)
        }
    }

    private fun taImotInnsendingPåSisteSak(
        hendelse: InnsendingMottattHendelse,
        sisteSakId: UUID,
    ) {
        val innsending =
            Innsending.opprett(hendelse = hendelse) { ident ->
                personMediator.finnEllerOpprettPerson(
                    hendelse.ident,
                )
            }
        innsendingRepository.lagre(innsending)
        val behandling =
            Behandling(
                behandlingId = innsending.innsendingId,
                opprettet = innsending.mottatt,
                hendelse = hendelse,
                utløstAv = UtløstAvType.INNSENDING,
            )

        sakMediator.knyttBehandlingTilSak(
            behandling = behandling,
            hendelse = hendelse,
            sakId = sisteSakId,
        )

        oppgaveMediator.lagOppgaveForInnsendingBehandling(
            innsendingMottattHendelse = hendelse,
            behandling = behandling,
            person = personMediator.finnEllerOpprettPerson(hendelse.ident),
        )
    }

    fun ferdigstill(hendelse: FerdigstillInnsendingHendelse) {
        val innsending = hentInnsending(innsendingId = hendelse.innsendingId, saksbehandler = hendelse.utførtAv)
        innsending.startFerdigstilling(hendelse)
        innsendingRepository.lagre(innsending)

        val innsendingFerdigstiltHendelse =
            innsendingBehandler.utførAksjon(innsending = innsending, hendelse = hendelse)
        logger.info { "Ferdigstiller innsending med id=${innsending.innsendingId} med aksjon=${hendelse.aksjon}" }

        //  Oppdater innsending med behandlingId fra aksjon og sett til ferdigstilt
        innsending.ferdigstill(innsendingFerdigstiltHendelse)
        oppgaveMediator.ferdigstillOppgave(innsendingFerdigstiltHendelse)

        innsendingRepository.lagre(innsending)
    }

    fun automatiskFerdigstill(hendelse: BehandlingOpprettetForSøknadHendelse) {
        val innsendinger = innsendingRepository.finnInnsendingerForPerson(ident = hendelse.ident)
        innsendinger
            .singleOrNull {
                it.gjelderSøknadMedId(søknadId = hendelse.søknadId)
            }?.let { innsending ->
                innsending.automatiskFerdigstill(hendelse)
                innsendingRepository.lagre(innsending)
                oppgaveMediator.avbrytOppgave(
                    hendelse =
                        BehandlingAvbruttHendelse(
                            behandlingId = innsending.innsendingId,
                            behandletHendelseId = hendelse.søknadId.toString(),
                            behandletHendelseType = "Søknad",
                            ident = hendelse.ident,
                            utførtAv = hendelse.utførtAv,
                        ),
                )
            }
    }

    fun hentInnsending(
        innsendingId: UUID,
        saksbehandler: Saksbehandler,
    ): Innsending =
        innsendingRepository.hent(innsendingId = innsendingId).let { innsending ->
            innsending.harTilgang(saksbehandler)
            innsending
        }

    fun hentLovligeSaker(ident: String): List<Sak> = sakMediator.finnSakHistorikk(ident)?.saker() ?: emptyList()
}
