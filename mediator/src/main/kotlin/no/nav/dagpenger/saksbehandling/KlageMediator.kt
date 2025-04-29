package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
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
    private val klageRepository: KlageRepository,
    private val oppgaveMediator: OppgaveMediator,
    private val utsendingMediator: UtsendingMediator,
) {
    fun hentKlageBehandling(behandlingId: UUID): KlageBehandling = klageRepository.hentKlageBehandling(behandlingId)

    fun opprettKlage(klageMottattHendelse: KlageMottattHendelse): UUID {
        val klageBehandling =
            KlageBehandling(
                behandlingId = UUIDv7.ny(),
            )

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
        sjekkTilgangOgEierAvOppgave(behandlingId, saksbehandler)
        klageRepository.hentKlageBehandling(behandlingId).let { klageBehandling ->
            when (verdi) {
                is OpplysningerVerdi.Tekst -> klageBehandling.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.TekstListe -> klageBehandling.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.Dato -> klageBehandling.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.Boolsk -> klageBehandling.svar(opplysningId, verdi.value)
            }
            klageRepository.lagre(klageBehandling = klageBehandling)
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

        val oppgave = sjekkTilgangOgEierAvOppgave(hendelse.behandlingId, hendelse.utførtAv)

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

    fun avbrytKlage(hendelse: AvbruttHendelse) {
        sjekkTilgangOgEierAvOppgave(hendelse.behandlingId, hendelse.utførtAv)
        val klageBehandling =
            klageRepository.hentKlageBehandling(hendelse.behandlingId).also { klageBehandling ->
                klageBehandling.avbryt()
            }

        klageRepository.lagre(klageBehandling)

        oppgaveMediator.ferdigstillOppgave(avbruttHendelse = hendelse)
    }

    private fun sjekkTilgangOgEierAvOppgave(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave {
        return oppgaveMediator.hentOppgaveHvisTilgang(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        ).also {
            requireEierAvOppgave(oppgave = it, saksbehandler = saksbehandler)
        }
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
