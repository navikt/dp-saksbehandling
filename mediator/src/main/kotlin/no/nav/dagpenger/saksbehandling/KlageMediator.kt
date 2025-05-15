package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class KlageMediator(
    private val klageRepository: KlageRepository,
    private val oppgaveMediator: OppgaveMediator,
    private val utsendingMediator: UtsendingMediator,
    private val saksbehandlerOppslag: SaksbehandlerOppslag,
) {
    private lateinit var rapidsConnection: RapidsConnection

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

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

    fun ferdigstill(hendelse: KlageFerdigbehandletHendelse) {
        val html = "<html><h1>Dette må vi gjøre noe med</h1></html>"

        val oppgave = sjekkTilgangOgEierAvOppgave(behandlingId = hendelse.behandlingId, saksbehandler = hendelse.utførtAv)

        val behandlendeEnhet =
            runBlocking {
                saksbehandlerOppslag.hentSaksbehandler(
                    navIdent = hendelse.utførtAv.navIdent,
                ).enhet.enhetNr
            }
        val klageBehandling =
            klageRepository.hentKlageBehandling(hendelse.behandlingId).also { klageBehandling ->
                klageBehandling.saksbehandlingFerdig(enhetsnummer = behandlendeEnhet)
            }

        // TODO: Fiks sak.... Den skal ikke lages her
        val sak =
            Sak(
                id = UUIDv7.ny().toString(),
                kontekst = "Dagpenger",
            )

        val startUtsendingHendelse =
            StartUtsendingHendelse(
                oppgaveId = oppgave.oppgaveId,
                sak = sak,
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
        if (klageBehandling.utfall() == UtfallType.OPPRETTHOLDELSE) {
            val message =
                JsonMessage.newNeed(
                    behov = setOf("OversendelseKlageinstans"),
                    map =
                        mapOf(
                            "behandlingId" to klageBehandling.behandlingId.toString(),
                            "ident" to oppgave.behandling.person.ident,
                            "fagsakId" to sak.id,
                        ),
                ).toJson().also {
                    sikkerlogg.info { "Publiserer behov: $it for oversendelse til klageinstans" }
                }
            logger.info { "Publiserer behov OversendelseKlageinstans for klagebehandling ${klageBehandling.behandlingId}" }
            rapidsConnection.publish(key = oppgave.behandling.person.ident, message = message)
        }
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

    fun oversendtTilKlageinstans(hendelse: OversendtKlageinstansHendelse) {
        klageRepository.hentKlageBehandling(behandlingId = hendelse.behandlingId).let { klageBehandling ->
            klageBehandling.oversendtTilKlageinstans()
            klageRepository.lagre(klageBehandling)
        }
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
