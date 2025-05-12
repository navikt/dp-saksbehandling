package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import java.util.UUID

class KlageMediator(
    private val klageRepository: KlageRepository,
    private val oppgaveMediator: OppgaveMediator,
    private val utsendingMediator: UtsendingMediator,
) {
    fun hentKlageBehandling(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): KlageBehandling {
        sjekkTilgangTilOppgave(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        )
        return klageRepository.hentKlageBehandling(behandlingId)
    }

    fun opprettKlage(klageMottattHendelse: KlageMottattHendelse): Oppgave {
        val klageBehandling =
            KlageBehandling(
                behandlingId = UUIDv7.ny(),
                journalpostId = klageMottattHendelse.journalpostId,
            )

        klageRepository.lagre(klageBehandling)
        return oppgaveMediator.opprettOppgaveForBehandling(
            behandlingOpprettetHendelse =
                BehandlingOpprettetHendelse(
                    behandlingId = klageBehandling.behandlingId,
                    ident = klageMottattHendelse.ident,
                    opprettet = klageMottattHendelse.opprettet,
                    type = BehandlingType.KLAGE,
                    utførtAv = klageMottattHendelse.utførtAv,
                ),
        )
    }

    fun oppdaterKlageOpplysning(
        behandlingId: UUID,
        opplysningId: UUID,
        verdi: Verdi,
        saksbehandler: Saksbehandler,
    ) {
        sjekkTilgangOgEierAvOppgave(behandlingId, saksbehandler)
        klageRepository.hentKlageBehandling(behandlingId).let { klageBehandling ->
            klageBehandling.svar(opplysningId, verdi)
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

    fun ferdigstill(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ) {
        val html = "<html><h1>Dette må vi gjøre noe med</h1></html>"

        val oppgave = sjekkTilgangOgEierAvOppgave(behandlingId = behandlingId, saksbehandler = saksbehandler)

        val klageBehandling =
            klageRepository.hentKlageBehandling(behandlingId).also { klageBehandling ->
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
                    utførtAv = saksbehandler,
                ),
        )
        utsendingMediator.mottaStartUtsending(
            startUtsendingHendelse,
        )
    }

    fun avbrytKlage(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ) {
        sjekkTilgangOgEierAvOppgave(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        )
        val klageBehandling =
            klageRepository.hentKlageBehandling(
                behandlingId = behandlingId,
            ).also { klageBehandling ->
                klageBehandling.avbryt()
            }

        klageRepository.lagre(klageBehandling)

        oppgaveMediator.ferdigstillOppgave(
            avbruttHendelse =
                AvbruttHendelse(
                    behandlingId = behandlingId,
                    utførtAv = saksbehandler,
                ),
        )
    }

    private fun sjekkTilgangTilOppgave(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave {
        return oppgaveMediator.hentOppgaveHvisTilgang(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        )
    }

    private fun sjekkTilgangOgEierAvOppgave(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave {
        return sjekkTilgangTilOppgave(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        ).also {
            requireEierAvOppgave(oppgave = it, saksbehandler = saksbehandler)
        }
    }
}
