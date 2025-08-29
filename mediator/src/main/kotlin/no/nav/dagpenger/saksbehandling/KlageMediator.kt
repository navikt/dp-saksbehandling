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
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.klage.Hjemler
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandsendring
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandslogg
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.INTERN_MELDING
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
                        KlageTilstandsendring(
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
                type = BehandlingType.KLAGE,
                utførtAv = klageMottattHendelse.utførtAv,
            )
        sakMediator.knyttTilSak(behandlingOpprettetHendelse = behandlingOpprettetHendelse)
        return kotlin.runCatching {
            oppgaveMediator.opprettOppgaveForBehandling(
                behandlingOpprettetHendelse = behandlingOpprettetHendelse,
            )
        }
            .onFailure { e ->
                logger.error { "Kunne ikke opprette oppgave for klagebehandling: ${klageBehandling.behandlingId}" }
                throw e
            }
            .getOrThrow()
    }

    fun opprettManuellKlage(manuellKlageMottattHendelse: ManuellKlageMottattHendelse): Oppgave {
        val klageBehandling =
            KlageBehandling(
                journalpostId = manuellKlageMottattHendelse.journalpostId,
                opprettet = manuellKlageMottattHendelse.opprettet,
                tilstandslogg =
                    KlageTilstandslogg(
                        KlageTilstandsendring(
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
                type = BehandlingType.KLAGE,
                utførtAv = utførtAv,
            )
        sakMediator.knyttTilSak(behandlingOpprettetHendelse = behandlingOpprettetHendelse)

        auditlogg.opprett(
            "Opprettet en manuell klage",
            manuellKlageMottattHendelse.ident,
            utførtAv.navIdent,
        )

        return kotlin.runCatching {
            oppgaveMediator.opprettOppgaveForBehandling(
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
        }
            .onFailure { e ->
                logger.error { "Kunne ikke opprette oppgave for klagebehandling: ${klageBehandling.behandlingId}" }
                throw e
            }
            .getOrThrow()
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

    fun ferdigstill(
        hendelse: KlageFerdigbehandletHendelse,
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
                        behandlingId = oppgave.behandlingId,
                        saksbehandlerToken = saksbehandlerToken,
                        behandlingType = BehandlingType.KLAGE,
                    )
                }
            val klageBehandling =
                klageRepository.hentKlageBehandling(hendelse.behandlingId).also { klageBehandling ->
                    klageBehandling.saksbehandlingFerdig(
                        behandlendeEnhet = saksbehandlerDeferred.await().enhet.enhetNr,
                        hendelse = hendelse,
                    )
                }
            val sakId = sakMediator.hentSakIdForBehandlingId(behandlingId = klageBehandling.behandlingId)

            val utsendingSak =
                UtsendingSak(
                    id = sakId.toString(),
                    kontekst = "Dagpenger",
                )

            val startUtsendingHendelse =
                StartUtsendingHendelse(
                    utsendingSak = utsendingSak,
                    behandlingId = klageBehandling.behandlingId,
                    ident = oppgave.personIdent(),
                )

            val html = htmlDeferred.await().getOrThrow()
            utsendingMediator.opprettUtsending(
                behandlingId = oppgave.behandlingId,
                brev = html,
                ident = oppgave.personIdent(),
                type = UtsendingType.KLAGEMELDING,
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

            val saksbehandler = saksbehandlerDeferred.await()
            if (klageBehandling.utfall() == UtfallType.OPPRETTHOLDELSE) {
                auditlogg.oppdater("Ferdigstilte en klage", oppgave.personIdent(), saksbehandler.ident)
                val body =
                    mutableMapOf(
                        "behandlingId" to klageBehandling.behandlingId.toString(),
                        "ident" to oppgave.personIdent(),
                        "fagsakId" to utsendingSak.id,
                        "behandlendeEnhet" to saksbehandler.enhet.enhetNr,
                        "hjemler" to klageBehandling.hjemler(),
                        "opprettet" to oppgave.opprettet.toLocalDate().toString(),
                    )

                klageBehandling.synligeOpplysninger()
                    .filter { OpplysningBygger.fullmektigTilKlageinstansOpplysningTyper.contains(it.type) && it.verdi() != Verdi.TomVerdi }
                    .forEach {
                        val verdi = (it.verdi() as Verdi.TekstVerdi).value
                        when (it.type) {
                            OpplysningType.FULLMEKTIG_NAVN -> body.put("prosessfullmektigNavn", verdi)
                            OpplysningType.FULLMEKTIG_ADRESSE_1 -> body.put("prosessfullmektigAdresselinje1", verdi)
                            OpplysningType.FULLMEKTIG_ADRESSE_2 -> body.put("prosessfullmektigAdresselinje2", verdi)
                            OpplysningType.FULLMEKTIG_ADRESSE_3 -> body.put("prosessfullmektigAdresselinje3", verdi)
                            OpplysningType.FULLMEKTIG_POSTNR -> body.put("prosessfullmektigPostnummer", verdi)
                            OpplysningType.FULLMEKTIG_POSTSTED -> body.put("prosessfullmektigPoststed", verdi)
                            OpplysningType.FULLMEKTIG_LAND -> body.put("prosessfullmektigLand", verdi)
                            else -> {}
                        }
                    }

                klageBehandling.synligeOpplysninger()
                    .singleOrNull { it.type == INTERN_MELDING && it.verdi() != Verdi.TomVerdi }
                    ?.let {
                        val verdi = (it.verdi() as Verdi.TekstVerdi).value
                        body.put("kommentar", verdi)
                    }

                val message =
                    JsonMessage.newNeed(
                        behov = setOf("OversendelseKlageinstans"),
                        map = body,
                    ).toJson().also {
                        sikkerlogg.info { "Publiserer behov: $it for oversendelse til klageinstans" }
                    }
                logger.info { "Publiserer behov OversendelseKlageinstans for klagebehandling ${klageBehandling.behandlingId}" }
                rapidsConnection.publish(key = oppgave.personIdent(), message = message)
            }
        }
    }

    fun avbrytKlage(hendelse: AvbruttHendelse) {
        sjekkTilgangOgEierAvOppgave(
            behandlingId = hendelse.behandlingId,
            saksbehandler = hendelse.utførtAv,
        )
        val klageBehandling =
            klageRepository.hentKlageBehandling(
                behandlingId = hendelse.behandlingId,
            ).also { klageBehandling ->
                klageBehandling.avbryt(hendelse = hendelse)
            }

        klageRepository.lagre(klageBehandling)
        oppgaveMediator.ferdigstillOppgave(avbruttHendelse = hendelse)
        auditlogg.oppdater("Avbrutte en klage", klageBehandling.personIdent(), hendelse.utførtAv.navIdent)
    }

    fun oversendtTilKlageinstans(hendelse: OversendtKlageinstansHendelse) {
        klageRepository.hentKlageBehandling(behandlingId = hendelse.behandlingId).let { klageBehandling ->
            klageBehandling.oversendtTilKlageinstans(hendelse)
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

fun KlageBehandling.hjemler(): List<String> {
    val verdi =
        this.synligeOpplysninger()
            .singleOrNull { it.type == OpplysningType.HJEMLER }?.verdi() as Verdi.Flervalg?
    return verdi?.value?.mapNotNull {
        Hjemler.values().find { hjemmel -> hjemmel.tittel == it }?.name
    }.orEmpty()
}
