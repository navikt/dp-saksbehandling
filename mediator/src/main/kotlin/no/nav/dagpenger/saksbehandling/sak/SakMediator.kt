package no.nav.dagpenger.saksbehandling.sak

import AdresseBeeskyttetPersonException
import PersonMediator
import SkjermetPersonException
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.KnyttTilSakResultat
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import java.util.UUID

private val logger = KotlinLogging.logger {}

class SakMediator(
    private val personMediator: PersonMediator,
    private val sakRepository: SakRepository,
) {
    private lateinit var rapidsConnection: RapidsConnection

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

    fun hentSakHistorikk(ident: String): SakHistorikk {
        return sakRepository.hentSakHistorikk(ident)
    }

    fun finnSakHistorikk(ident: String): SakHistorikk? {
        return sakRepository.finnSakHistorikk(ident)
    }

    fun opprettSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse): Sak {
        val sakId =
            requireNotNull(søknadsbehandlingOpprettetHendelse.behandlingskjedeId) {
                logger.error {
                    "Mottok SøknadsbehandlingOpprettetHendelse uten behandlingskjedeId for " +
                        "behandlingId ${søknadsbehandlingOpprettetHendelse.behandlingId}"
                }
            }
        val sak =
            Sak(
                sakId = sakId,
                søknadId = søknadsbehandlingOpprettetHendelse.søknadId,
                opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
            ).also {
                it.leggTilBehandling(
                    Behandling(
                        behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
                        utløstAv = UtløstAvType.SØKNAD,
                        opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
                        hendelse = søknadsbehandlingOpprettetHendelse,
                    ),
                )
            }

        runCatching {
            personMediator.finnEllerOpprettPerson(søknadsbehandlingOpprettetHendelse.ident)
        }.onFailure { e ->
            when (e is AdresseBeeskyttetPersonException || e is SkjermetPersonException) {
                true -> {
                    sendAvbrytBehandling(søknadsbehandlingOpprettetHendelse = søknadsbehandlingOpprettetHendelse)
                }

                else -> {
                    throw e
                }
            }
        }.onSuccess { person ->
            val sakHistorikk =
                sakRepository.finnSakHistorikk(søknadsbehandlingOpprettetHendelse.ident) ?: SakHistorikk(
                    person = person,
                )
            sakHistorikk.leggTilSak(sak)
            sakRepository.lagre(sakHistorikk)
        }
        return sak
    }

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse) {
        sakRepository.hentSakHistorikk(meldekortbehandlingOpprettetHendelse.ident).also {
            it.knyttTilSak(meldekortbehandlingOpprettetHendelse).also { resultat ->
                sjekkResultat(
                    meldekortbehandlingOpprettetHendelse.behandlingId,
                    meldekortbehandlingOpprettetHendelse.javaClass.simpleName,
                    resultat,
                )
            }
            sakRepository.lagre(it)
        }
    }

    fun knyttTilSak(manuellBehandlingOpprettetHendelse: ManuellBehandlingOpprettetHendelse) {
        sakRepository.hentSakHistorikk(manuellBehandlingOpprettetHendelse.ident).also {
            it.knyttTilSak(manuellBehandlingOpprettetHendelse).also { resultat ->
                sjekkResultat(
                    manuellBehandlingOpprettetHendelse.behandlingId,
                    manuellBehandlingOpprettetHendelse.javaClass.simpleName,
                    resultat,
                )
            }
            sakRepository.lagre(it)
        }
    }

    fun knyttTilSak(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        sakRepository.hentSakHistorikk(behandlingOpprettetHendelse.ident).also {
            it.knyttTilSak(behandlingOpprettetHendelse = behandlingOpprettetHendelse).also {
                    resultat ->
                sjekkResultat(
                    behandlingOpprettetHendelse.behandlingId,
                    behandlingOpprettetHendelse.javaClass.simpleName,
                    resultat,
                )
            }
            sakRepository.lagre(it)
        }
    }

    fun knyttTilSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        sakRepository.hentSakHistorikk(søknadsbehandlingOpprettetHendelse.ident).also {
            it.knyttTilSak(søknadsbehandlingOpprettetHendelse).also { resultat ->
                sjekkResultat(
                    søknadsbehandlingOpprettetHendelse.behandlingId,
                    søknadsbehandlingOpprettetHendelse.javaClass.simpleName,
                    resultat,
                )
            }
            sakRepository.lagre(it)
        }
    }

    fun oppdaterSakMedArenaSakId(vedtakFattetHendelse: VedtakFattetHendelse) {
        val sak = vedtakFattetHendelse.sak
        require(sak != null) { "VedtakFattetHendelse må ha en sak" }

        if (sak.kontekst == "Arena") {
            sakRepository.hentSakIdForBehandlingId(vedtakFattetHendelse.behandlingId).let { sakId ->
                sakRepository.settArenaSakId(
                    sakId = sakId,
                    arenaSakId = sak.id,
                )
            }
        }
    }

    fun merkSakenSomDpSak(vedtakFattetHendelse: VedtakFattetHendelse) {
        sakRepository.hentSakIdForBehandlingId(vedtakFattetHendelse.behandlingId).let { sakId ->
            sakRepository.merkSakenSomDpSak(
                sakId = sakId,
                erDpSak = true,
            )
        }
    }

    fun finnSisteSakId(ident: String): UUID? {
        return sakRepository.finnSisteSakId(ident)
    }

    fun finnSakIdForSøknad(
        søknadId: UUID,
        ident: String,
    ): UUID? {
        return sakRepository.finnSakIdForSøknad(søknadId = søknadId, ident = ident)
    }

    fun hentSakIdForBehandlingId(behandlingId: UUID): UUID {
        return sakRepository.hentSakIdForBehandlingId(behandlingId)
    }

    fun hentDagpengerSakIdForBehandlingId(behandlingId: UUID): UUID {
        return sakRepository.hentDagpengerSakIdForBehandlingId(behandlingId)
    }

    private fun sendAvbrytBehandling(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        rapidsConnection.publish(
            key = søknadsbehandlingOpprettetHendelse.ident,
            message =
                JsonMessage.newMessage(
                    eventName = "avbryt_behandling",
                    map =
                        mapOf(
                            "behandlingId" to søknadsbehandlingOpprettetHendelse.behandlingId,
                            "søknadId" to søknadsbehandlingOpprettetHendelse.søknadId,
                            "ident" to søknadsbehandlingOpprettetHendelse.ident,
                        ),
                ).toJson(),
        )
        logger.info { "Publiserte avbryt_behandling hendelse" }
    }

    private fun SakMediator.sjekkResultat(
        behandlingId: UUID,
        hendelseType: String,
        resultat: KnyttTilSakResultat,
    ) {
        when (resultat) {
            is KnyttTilSakResultat.KnyttetTilSak -> {
                logger.info { "Knyttet behandlingId: $behandlingId til sakId: ${resultat.sak.sakId}" }
            }
            else -> {
                logger.warn { "Klarte ikke å knytte behandlingId: $behandlingId av type $hendelseType til noen sak" }
                rapidsConnection.sendAlertTilRapid(
                    feilType =
                        AlertManager.KnytningTilSakFeil(
                            behandlingId = behandlingId,
                            hendelseType = hendelseType,
                            knyttTilSakResultat = resultat,
                        ),
                    utvidetFeilMelding = null,
                )
            }
        }
    }

    fun knyttEttersendingTilSammeSakSomSøknad(
        behandling: Behandling,
        hendelse: InnsendingMottattHendelse,
    ) {
        requireNotNull(hendelse.søknadId) { "Ettersending må ha søknadId for å knyttes til samme sak som søknaden" }
        val sakId =
            finnSakIdForSøknad(søknadId = hendelse.søknadId!!, ident = hendelse.ident)
                ?: throw IllegalStateException("Fant ingen sak for søknadId: ${hendelse.søknadId}")
        sakRepository.finnSakHistorikk(ident = hendelse.ident).let { sakHistorikk ->
            sakHistorikk?.saker()?.find { sak -> sak.sakId == sakId }?.let { sak ->
                sak.leggTilBehandling(behandling)
                sakRepository.lagre(sakHistorikk)
            } ?: throw IllegalStateException("Fant ingen sak for søknadId: ${hendelse.søknadId}")
        }
    }

    fun knyttBehandlingTilSak(
        behandling: Behandling,
        hendelse: InnsendingMottattHendelse,
        sakId: UUID,
    ) {
        sakRepository.finnSakHistorikk(ident = hendelse.ident).let { sakHistorikk ->
            sakHistorikk?.saker()?.find { sak ->
                sak.sakId == sakId
            }?.let { sak ->
                sak.leggTilBehandling(
                    behandling = behandling,
                )
                sakRepository.lagre(sakHistorikk)
            } ?: throw IllegalStateException("Fant ikke sak med id: $sakId")
        }
    }
}
