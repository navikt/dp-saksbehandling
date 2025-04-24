package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.db.klage.InmemoryKlageRepository
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillKlageOppgave
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import java.time.LocalDate
import java.util.UUID

class KlageMediator(
    private val klageRepository: KlageRepository = InmemoryKlageRepository,
    private val oppgaveMediator: OppgaveMediator,
    private val utsendingMediator: UtsendingMediator,
) {
    fun hentKlageBehandling(behandlingId: UUID): KlageBehandling = klageRepository.hentKlageBehandling(behandlingId)

    fun opprettKlage(klageMottattHendelse: KlageMottattHendelse): UUID {
        val klageBehandling =
            KlageBehandling(
                behandlingId = UUIDv7.ny(),
            )

        // TODO Lag en Alert-jobb som sjekker om det finnes klager uten oppgave, som kan fange om hvis klage er
        // lagret og oppretting av oppgave feiler
        klageRepository.lagre(klageBehandling)
        oppgaveMediator.opprettOppgaveForBehandling(
            behandlingOpprettetHendelse =
                BehandlingOpprettetHendelse(
                    behandlingId = klageBehandling.behandlingId,
                    ident = klageMottattHendelse.ident,
                    opprettet = klageMottattHendelse.opprettet,
                    type = BehandlingType.KLAGE,
                    utførtAv = klageMottattHendelse.utførtAv,
                ),
        )
        return klageBehandling.behandlingId
    }

    fun oppdaterKlageOpplysning(
        behandlingId: UUID,
        opplysningId: UUID,
        verdi: OpplysningerVerdi,
        saksbehandler: Saksbehandler,
    ) {
        oppgaveMediator.hentOppgaveHvisTilgang(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        ).also {
            requireEierAvOppgave(oppgave = it, saksbehandler = saksbehandler)
        }
        klageRepository.hentKlageBehandling(behandlingId).let { klageBehandling ->
            when (verdi) {
                is OpplysningerVerdi.Tekst -> klageBehandling.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.TekstListe -> klageBehandling.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.Dato -> klageBehandling.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.Boolsk -> klageBehandling.svar(opplysningId, verdi.value)
            }
        }
    }

    private fun requireEierAvOppgave(
        oppgave: Oppgave,
        saksbehandler: Saksbehandler,
    ) {
        require(oppgave.erEierAvOppgave(saksbehandler)) {
            "Saksbehandler ${saksbehandler.navIdent} må eie oppgaven ${oppgave.oppgaveId}"
        }
    }

    fun ferdigstill(hendelse: FerdigstillKlageOppgave) {
        val html = "må hente html"

        val oppgave =
            oppgaveMediator.hentOppgaveHvisTilgang(
                behandlingId = hendelse.behandlingId,
                saksbehandler = hendelse.utførtAv,
            ).also {
                requireEierAvOppgave(oppgave = it, saksbehandler = hendelse.utførtAv)
            }

        val klageBehandling =
            klageRepository.hentKlageBehandling(hendelse.behandlingId).also { klageBehandling ->
                klageBehandling.ferdigstill()
            }

        val startUtsendingHendelse =
            StartUtsendingHendelse(
                oppgaveId = oppgave.oppgaveId,
                sak =
                    Sak(
                        id = UUIDv7.ny().toString(),
                        kontekst = "Dagpenger",
                    ),
                behandlingId = klageBehandling.behandlingId,
                ident = oppgave.behandling.person.ident,
            )

        utsendingMediator.opprettUtsending(
            oppgaveId = oppgave.oppgaveId,
            brev = html,
            ident = oppgave.behandling.person.ident,
        )

        klageRepository.lagre(klageBehandling)
        oppgaveMediator.ferdigstillOppgave(
            godkjentBehandlingHendelse =
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = html,
                    utførtAv = hendelse.utførtAv,
                ),
        )
        utsendingMediator.mottaStartUtsending(
            startUtsendingHendelse,
        )
    }
}

sealed class OpplysningerVerdi {
    data class Tekst(val value: String) : OpplysningerVerdi()

    data class TekstListe(val value: List<String> = emptyList()) : OpplysningerVerdi(), List<String> by value {
        constructor(vararg values: String) : this(values.toList())
    }

    data class Dato(val value: LocalDate) : OpplysningerVerdi()

    data class Boolsk(val value: Boolean) : OpplysningerVerdi()

    companion object
}
