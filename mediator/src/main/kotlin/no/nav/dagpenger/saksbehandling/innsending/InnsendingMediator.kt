package no.nav.dagpenger.saksbehandling.innsending

import PersonMediator
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.innsending.InnsendingRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

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

        if (skalEttersendingTilSøknadVarsles) {
            taImotEttersendingTilSøknad(hendelse)
        } else if (sisteSakId != null) {
            taImotInnsendingPåSisteSak(hendelse, sisteSakId)
        }
        return when (sisteSakId) {
            null -> HåndterInnsendingResultat.UhåndtertInnsending
            else -> HåndterInnsendingResultat.HåndtertInnsending(sisteSakId)
        }
    }

    private fun taImotEttersendingTilSøknad(hendelse: InnsendingMottattHendelse) {
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
        oppgaveMediator.lagOppgaveForInnsendingBehandling(
            innsendingMottattHendelse = hendelse,
            behandling = behandling,
            person = person,
        )
    }

    private fun taImotInnsendingPåSisteSak(
        hendelse: InnsendingMottattHendelse,
        sisteSakId: UUID,
    ) {
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
        sakMediator.knyttBehandlingTilSak(
            behandling = behandling,
            hendelse = hendelse,
            sakId = sisteSakId,
        )
        oppgaveMediator.lagOppgaveForInnsendingBehandling(
            innsendingMottattHendelse = hendelse,
            behandling = behandling,
            person = person,
        )
    }

    fun ferdigstill(hendelse: FerdigstillInnsendingHendelse) {
        val innsending = hentInnsending(innsendingId = hendelse.innsendingId, saksbehandler = hendelse.utførtAv)
        innsending.startFerdigstilling(hendelse)
        innsendingRepository.lagre(innsending)

        val innsendingFerdigstiltHendelse =
            innsendingBehandler.utførAksjon(innsending = innsending, hendelse = hendelse)
        logger.info { "Ferdigstiller innsending med id=${innsending.innsendingId} med aksjon=${hendelse.aksjon}" }

        //  Oppdater innsending med behandlingId fra aksjon
        innsending.ferdigstill(innsendingFerdigstiltHendelse)
        oppgaveMediator.ferdigstillOppgave(innsendingFerdigstiltHendelse)

        // Setter tilstand "Ferdigstilt"
        innsendingRepository.lagre(innsending)
    }

    fun avbrytInnsending(hendelse: BehandlingOpprettetForSøknadHendelse) {
        val innsendinger = innsendingRepository.finnInnsendingerForPerson(ident = hendelse.ident)
        innsendinger.singleOrNull {
            it.gjelderSøknadMedId(søknadId = hendelse.søknadId)
        }?.let { innsending ->

            // TODO: sett nyBehandling på innsendingen
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
    ): Innsending {
        return innsendingRepository.hent(innsendingId = innsendingId).let { innsending ->
            innsending.harTilgang(saksbehandler)
            innsending
        }
    }
}
