package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.audit.Auditlogg
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageBehandlingUtført
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.klage.KlageAksjon
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandslogg
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingType
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class KlageMediator(
    private val klageRepository: KlageRepository,
    private val oppgaveMediator: OppgaveMediator,
    private val utsendingMediator: UtsendingMediator,
    private val oppslag: Oppslag,
    private val meldingOmVedtakKlient: MeldingOmVedtakKlient,
    private val sakMediator: SakMediator,
) {
    private lateinit var rapidsConnection: RapidsConnection
    private lateinit var auditlogg: Auditlogg

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

    fun setAuditlogg(auditlogg: Auditlogg) {
        this.auditlogg = auditlogg
    }

    fun hentKlageBehandling(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): KlageBehandling {
        sjekkTilgangTilOppgave(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        )
        val klageBehandling = klageRepository.hentKlageBehandling(behandlingId)
        auditlogg.les("Så en klagebehandling", klageBehandling.personIdent(), saksbehandler.navIdent)
        return klageBehandling
    }

    fun opprettKlage(klageMottattHendelse: KlageMottattHendelse): Oppgave {
        // todo her kan en Exception kastes hvis personen ikke finnes

        val klageBehandling =
            KlageBehandling(
                journalpostId = klageMottattHendelse.journalpostId,
                opprettet = klageMottattHendelse.opprettet,
                tilstandslogg =
                    KlageTilstandslogg(
                        Tilstandsendring(
                            tilstand = BEHANDLES,
                            hendelse = klageMottattHendelse,
                        ),
                    ),
            )
        klageRepository.lagre(klageBehandling = klageBehandling)

        val behandlingOpprettetHendelse =
            BehandlingOpprettetHendelse(
                behandlingId = klageBehandling.behandlingId,
                ident = klageMottattHendelse.ident,
                sakId = klageMottattHendelse.sakId,
                opprettet = klageMottattHendelse.opprettet,
                type = UtløstAvType.KLAGE,
                utførtAv = klageMottattHendelse.utførtAv,
            )
        sakMediator.knyttTilSak(behandlingOpprettetHendelse = behandlingOpprettetHendelse)
        return runCatching {
            oppgaveMediator.opprettOppgaveForKlageBehandling(
                behandlingOpprettetHendelse = behandlingOpprettetHendelse,
            )
        }.onFailure { e ->
            logger.error { "Kunne ikke opprette oppgave for klagebehandling: ${klageBehandling.behandlingId}" }
            throw e
        }.getOrThrow()
    }

    fun opprettManuellKlage(manuellKlageMottattHendelse: ManuellKlageMottattHendelse): Oppgave {
        val klageBehandling =
            KlageBehandling(
                journalpostId = manuellKlageMottattHendelse.journalpostId,
                opprettet = manuellKlageMottattHendelse.opprettet,
                tilstandslogg =
                    KlageTilstandslogg(
                        Tilstandsendring(
                            tilstand = BEHANDLES,
                            hendelse = manuellKlageMottattHendelse,
                        ),
                    ),
            )

        klageRepository.lagre(klageBehandling)

        val utførtAv = manuellKlageMottattHendelse.utførtAv
        val behandlingOpprettetHendelse =
            BehandlingOpprettetHendelse(
                behandlingId = klageBehandling.behandlingId,
                ident = manuellKlageMottattHendelse.ident,
                sakId = manuellKlageMottattHendelse.sakId,
                opprettet = manuellKlageMottattHendelse.opprettet,
                type = UtløstAvType.KLAGE,
                utførtAv = utførtAv,
            )
        sakMediator.knyttTilSak(behandlingOpprettetHendelse = behandlingOpprettetHendelse)

        auditlogg.opprett(
            "Opprettet en manuell klage",
            manuellKlageMottattHendelse.ident,
            utførtAv.navIdent,
        )

        return runCatching {
            oppgaveMediator
                .opprettOppgaveForKlageBehandling(
                    behandlingOpprettetHendelse = behandlingOpprettetHendelse,
                ).also { oppgave ->
                    oppgaveMediator.tildelOppgave(
                        SettOppgaveAnsvarHendelse(
                            oppgaveId = oppgave.oppgaveId,
                            ansvarligIdent = utførtAv.navIdent,
                            utførtAv = utførtAv,
                        ),
                    )
                }
        }.onFailure { e ->
            logger.error { "Kunne ikke opprette oppgave for klagebehandling: ${klageBehandling.behandlingId}" }
            throw e
        }.getOrThrow()
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
            auditlogg.oppdater("Oppdaterte en klageopplysning", klageBehandling.personIdent(), saksbehandler.navIdent)
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

    fun behandlingUtført(
        hendelse: KlageBehandlingUtført,
        saksbehandlerToken: String,
    ) {
        val oppgave =
            sjekkTilgangOgEierAvOppgave(behandlingId = hendelse.behandlingId, saksbehandler = hendelse.utførtAv)

        runBlocking {
            val saksbehandlerDeferred =
                async(Dispatchers.IO) {
                    oppslag.hentBehandler(
                        ident = hendelse.utførtAv.navIdent,
                    )
                }

            val personDeferred =
                async(Dispatchers.IO) {
                    oppslag.hentPerson(oppgave.personIdent())
                }

            val htmlDeferred =
                async(Dispatchers.IO) {
                    meldingOmVedtakKlient.lagOgHentMeldingOmVedtak(
                        person = personDeferred.await(),
                        saksbehandler = saksbehandlerDeferred.await(),
                        beslutter = null,
                        behandlingId = oppgave.behandling.behandlingId,
                        saksbehandlerToken = saksbehandlerToken,
                        utløstAvType = UtløstAvType.KLAGE,
                    )
                }

            val klageBehandling =
                klageRepository.hentKlageBehandling(hendelse.behandlingId).also { klageBehandling ->
                    klageBehandling.behandlingUtført(
                        behandlendeEnhet = saksbehandlerDeferred.await().enhet.enhetNr,
                        hendelse = hendelse,
                    )
                }
            val sakId = sakMediator.hentSakIdForBehandlingId(behandlingId = klageBehandling.behandlingId)
//            1. commit - oppretter utsending
            val html = htmlDeferred.await().getOrThrow()
            utsendingMediator.opprettUtsending(
                behandlingId = oppgave.behandling.behandlingId,
                brev = html,
                ident = oppgave.personIdent(),
                type = UtsendingType.KLAGEMELDING,
            )

//            2. commit - lagrer klagebehandling
            klageRepository.lagre(klageBehandling)
//            2. publisher på rapids - lagrer klagebehandling

            rapidsConnection.publish(
                oppgave.personIdent(),
                JsonMessage
                    .newMessage(
                        mapOf(
                            "@event_name" to "klage_behandling_utført",
                            "behandlingId" to klageBehandling.behandlingId,
                            "sakId" to sakId,
                            "ident" to oppgave.personIdent(),
                            "utfall" to klageBehandling.utfall()!!.name,
                            "saksbehandler" to hendelse.utførtAv,
                        ),
                    ).toJson(),
            )
        }
    }

    // TODO : Vurder om man bør bruke AvbrytOppgaveHendelse og sette oppgave til Avbrutt i stedet for Ferdigbehandlet
// TODO: Alternativt bør AvbruttHendelse renames til AvbrytKlageHendelse, siden den ikke skal brukes på andre type behandlinger
    fun avbrytKlage(hendelse: AvbruttHendelse) {
        sjekkTilgangOgEierAvOppgave(
            behandlingId = hendelse.behandlingId,
            saksbehandler = hendelse.utførtAv,
        )
        val klageBehandling =
            klageRepository
                .hentKlageBehandling(
                    behandlingId = hendelse.behandlingId,
                ).also { klageBehandling ->
                    klageBehandling.avbryt(hendelse = hendelse)
                }

        klageRepository.lagre(klageBehandling)
        oppgaveMediator.ferdigstillOppgave(avbruttHendelse = hendelse)
        // TODO: Fix skrivefeil i auditlogg
        auditlogg.oppdater("Avbrutte en klage", klageBehandling.personIdent(), hendelse.utførtAv.navIdent)
    }

    fun oversendtTilKlageinstans(hendelse: OversendtKlageinstansHendelse) {
        klageRepository.hentKlageBehandling(behandlingId = hendelse.behandlingId).let { klageBehandling ->
            klageBehandling.oversendtTilKlageinstans(hendelse)
            klageRepository.lagre(klageBehandling)
        }
    }

    // Her kommer en kafka hendelse når utsending er distribuert
    fun vedtakDistribuert(hendelse: UtsendingDistribuert) {
        val klageBehandling = klageRepository.hentKlageBehandling(behandlingId = hendelse.behandlingId)
        val sakId = sakMediator.hentSakIdForBehandlingId(behandlingId = klageBehandling.behandlingId)
        val finnJournalpostIdForBehandling: (UUID?) -> String? = { behandlingId ->
            when (behandlingId == null) {
                true -> null
                false -> utsendingMediator.finnUtsendingForBehandlingId(behandlingId)?.journalpostId()
            }
        }

        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
            )
        klageRepository.lagre(klageBehandling)

        when (aksjon) {
            is KlageAksjon.OversendKlageinstans -> {
                rapidsConnection.publish(
                    key = hendelse.ident,
                    message =
                        KlageInstansBehovBygger(
                            klageBehandling = klageBehandling,
                            sakId = sakId,
                            hendelse = hendelse,
                            finnJournalpostIdForBehandling = finnJournalpostIdForBehandling,
                        ).behov,
                )
            }

            is KlageAksjon.IngenAksjon -> {
                // Ingen handling nødvendig
            }
        }
    }

    private fun sjekkTilgangTilOppgave(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave =
        oppgaveMediator.hentOppgaveHvisTilgang(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        )

    private fun sjekkTilgangOgEierAvOppgave(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave =
        sjekkTilgangTilOppgave(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        ).also {
            requireEierAvOppgave(oppgave = it, saksbehandler = saksbehandler)
        }
}
