package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageBehandlingUtført
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageinstansVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.klage.KlageAksjon
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandslogg
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingType
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class KlageMediator(
    private val transaksjoner: Transaksjoner,
    private val klageRepository: KlageRepository,
    private val oppgaveMediator: OppgaveMediator,
    private val utsendingMediator: UtsendingMediator,
    private val oppslag: Oppslag,
    private val meldingOmVedtakKlient: MeldingOmVedtakKlient,
    private val sakMediator: SakMediator,
    private val rapidsConnection: RapidsConnection,
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

        val behandlingOpprettetHendelse =
            BehandlingOpprettetHendelse(
                behandlingId = klageBehandling.behandlingId,
                ident = klageMottattHendelse.ident,
                sakId = klageMottattHendelse.sakId,
                opprettet = klageMottattHendelse.opprettet,
                type = HendelseBehandler.Intern.Klage,
                utførtAv = klageMottattHendelse.utførtAv,
            )

        val oppgave =
            transaksjoner.transaksjon { ctx ->
                klageRepository.lagre(klageBehandling, ctx)
                val sakHistorikk = sakMediator.knyttTilSak(behandlingOpprettetHendelse, ctx)
                oppgaveMediator.lagOppgaveForKlage(
                    hendelse = behandlingOpprettetHendelse,
                    sakHistorikk = sakHistorikk,
                    ctx = ctx,
                )
            }

        logger.info { "Klagebehandling ${klageBehandling.behandlingId} opprettet med oppgave ${oppgave.oppgaveId}" }

        try {
            rapidsConnection.publish(
                key = klageMottattHendelse.ident,
                message =
                    JsonMessage
                        .newMessage(
                            mapOf(
                                "@event_name" to "klage_behandling_opprettet",
                                "behandlingId" to klageBehandling.behandlingId,
                                "sakId" to klageMottattHendelse.sakId,
                                "ident" to klageMottattHendelse.ident,
                                "mottatt" to klageMottattHendelse.opprettet,
                            ).let { base ->
                                klageMottattHendelse.journalpostId?.let { journalpostId ->
                                    base + ("journalpostId" to journalpostId)
                                } ?: base
                            },
                        ).toJson(),
            )
        } catch (e: Exception) {
            logger.error(e) {
                "Kunne ikke publisere klage_behandling_opprettet for ${klageBehandling.behandlingId}, men DB er OK"
            }
        }

        return oppgave
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

        val utførtAv = manuellKlageMottattHendelse.utførtAv
        val behandlingOpprettetHendelse =
            BehandlingOpprettetHendelse(
                behandlingId = klageBehandling.behandlingId,
                ident = manuellKlageMottattHendelse.ident,
                sakId = manuellKlageMottattHendelse.sakId,
                opprettet = manuellKlageMottattHendelse.opprettet,
                type = HendelseBehandler.Intern.Klage,
                utførtAv = utførtAv,
            )

        return transaksjoner.transaksjon { ctx ->
            klageRepository.lagre(klageBehandling, ctx)
            val sakHistorikk = sakMediator.knyttTilSak(behandlingOpprettetHendelse, ctx)
            oppgaveMediator.lagOppgaveForKlage(
                hendelse = behandlingOpprettetHendelse,
                sakHistorikk = sakHistorikk,
                saksbehandler = utførtAv,
                ctx = ctx,
            )
        }
    }

    fun oppdaterKlageOpplysning(
        behandlingId: UUID,
        opplysningId: UUID,
        verdi: Verdi,
        saksbehandler: Saksbehandler,
    ): KlageBehandling {
        sjekkTilgangOgEierAvOppgave(behandlingId, saksbehandler)
        return klageRepository.hentKlageBehandling(behandlingId).also { klageBehandling ->
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

    fun behandlingUtført(
        hendelse: KlageBehandlingUtført,
        saksbehandlerToken: String,
    ): KlageBehandling {
        val oppgave =
            sjekkTilgangOgEierAvOppgave(behandlingId = hendelse.behandlingId, saksbehandler = hendelse.utførtAv)

        return runBlocking {
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
            val klageBehandling =
                klageRepository.hentKlageBehandling(hendelse.behandlingId).also { klageBehandling ->
                    klageBehandling.behandlingUtført(
                        behandlendeEnhet = saksbehandlerDeferred.await().enhet.enhetNr,
                        hendelse = hendelse,
                    )
                }
            val sakId = sakMediator.hentSakIdForBehandlingId(behandlingId = klageBehandling.behandlingId)

            val utsendingType =
                when (klageBehandling.utfall()) {
                    UtfallType.OPPRETTHOLDELSE -> UtsendingType.KLAGE_OVERSENDELSE
                    UtfallType.AVVIST -> UtsendingType.KLAGE_AVVIST
                    else -> null
                }

            val html =
                utsendingType?.let {
                    async(Dispatchers.IO) {
                        meldingOmVedtakKlient.lagOgHentMeldingOmVedtak(
                            person = personDeferred.await(),
                            saksbehandler = saksbehandlerDeferred.await(),
                            beslutter = null,
                            behandlingId = oppgave.behandling.behandlingId,
                            saksbehandlerToken = saksbehandlerToken,
                            utløstAvType = HendelseBehandler.Intern.Klage,
                            sakId = sakId.toString(),
                        )
                    }.await().getOrThrow()
                }

            transaksjoner.transaksjon { ctx ->
                if (utsendingType != null && html != null) {
                    utsendingMediator.opprettUtsending(
                        behandlingId = oppgave.behandling.behandlingId,
                        brev = html,
                        ident = oppgave.personIdent(),
                        type = utsendingType,
                        ctx = ctx,
                    )
                }
                klageRepository.lagre(klageBehandling, ctx)
                oppgaveMediator.ferdigstillOppgave(
                    behandlingId = hendelse.behandlingId,
                    saksbehandler = hendelse.utførtAv,
                    ctx = ctx,
                )
            }

            logger.info { "Klagebehandling ${klageBehandling.behandlingId} utført — utsending, klage og oppgave lagret i transaksjon" }

            if (utsendingType != null) {
                utsendingMediator.mottaStartUtsending(
                    StartUtsendingHendelse(
                        behandlingId = oppgave.behandling.behandlingId,
                        utsendingSak =
                            UtsendingSak(
                                id = sakId.toString(),
                                kontekst = "Dagpenger",
                            ),
                        ident = oppgave.personIdent(),
                    ),
                )
            }

            klageBehandling
        }
    }

    // TODO : Vurder om man bør bruke AvbrytOppgaveHendelse og sette oppgave til Avbrutt i stedet for Ferdigbehandlet
    // TODO: Alternativt bør AvbruttHendelse renames til AvbrytKlageHendelse, siden den ikke skal brukes på andre type behandlinger
    fun avbrytKlage(hendelse: AvbruttHendelse): KlageBehandling {
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

        transaksjoner.transaksjon { ctx ->
            klageRepository.lagre(klageBehandling, ctx)
            oppgaveMediator.ferdigstillOppgave(avbruttHendelse = hendelse, ctx = ctx)
        }
        logger.info { "Klagebehandling ${klageBehandling.behandlingId} avbrutt — klage og oppgave lagret i transaksjon" }
        return klageBehandling
    }

    fun oversendtTilKlageinstans(hendelse: OversendtKlageinstansHendelse) {
        klageRepository.hentKlageBehandling(behandlingId = hendelse.behandlingId).let { klageBehandling ->
            klageBehandling.oversendtTilKlageinstans(hendelse)
            klageRepository.lagre(klageBehandling)
        }
    }

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
                        BehovbyggerKlageinstans(
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

    fun mottaKlageinstansVedtak(klageinstansVedtakHendelse: KlageinstansVedtakHendelse) {
        klageRepository.hentKlageBehandling(behandlingId = klageinstansVedtakHendelse.klageId).let { klageBehandling ->
            klageBehandling.mottaKlageinstansVedtak(klageinstansVedtakHendelse)
            klageRepository.lagre(klageBehandling)
        }
    }

    private fun sjekkTilgangTilOppgave(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave =
        oppgaveMediator.hentOppgaveFor(
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
