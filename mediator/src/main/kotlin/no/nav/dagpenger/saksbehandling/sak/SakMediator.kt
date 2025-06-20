package no.nav.dagpenger.saksbehandling.sak

import AdresseBeeskyttetPersonException
import PersonMediator
import SkjermetPersonException
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.NySak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import java.util.UUID

private val logger = mu.KotlinLogging.logger {}

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

    fun finnSakHistorikkk(ident: String): SakHistorikk? {
        return sakRepository.finnSakHistorikk(ident)
    }

    fun opprettSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse): NySak {
        val sak =
            NySak(
                søknadId = søknadsbehandlingOpprettetHendelse.søknadId,
                opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
            ).also {
                it.leggTilBehandling(
                    Behandling(
                        behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
                        type = BehandlingType.RETT_TIL_DAGPENGER,
                        opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
                        hendelse = søknadsbehandlingOpprettetHendelse,
                    ),
                )
            }

        kotlin.runCatching {
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
            it.knyttTilSak(meldekortbehandlingOpprettetHendelse)
            sakRepository.lagre(it)
        }
    }

    fun knyttTilSak(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        sakRepository.hentSakHistorikk(behandlingOpprettetHendelse.ident).also {
            it.knyttTilSak(behandlingOpprettetHendelse = behandlingOpprettetHendelse)
            sakRepository.lagre(it)
        }
    }

    fun hentSakIdForBehandlingId(behandlingId: UUID): UUID {
        return sakRepository.hentSakIdForBehandlingId(behandlingId)
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
}
