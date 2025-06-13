package no.nav.dagpenger.saksbehandling.sak

import AdresseBeeskyttetPersonException
import PersonMediator
import SkjermetPersonException
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.NyBehandling
import no.nav.dagpenger.saksbehandling.NySak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse

private val logger = mu.KotlinLogging.logger {}
class SakMediator(
    private val personMediator: PersonMediator,
    private val sakRepository: SakRepository,
    private val rapidsConnection: RapidsConnection,
) {
    fun hentSakHistorikk(ident: String): SakHistorikk {
        return sakRepository.hentSakHistorikk(ident)
    }

    fun finnSak(ident: String): SakHistorikk? {
        return sakRepository.finn(ident)
    }

    fun opprettSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        val sak =
            NySak(
                søknadId = søknadsbehandlingOpprettetHendelse.søknadId,
                opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
            ).also {
                it.leggTilBehandling(
                    NyBehandling(
                        behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
                        behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
                        opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
                    ),
                )
            }

        kotlin.runCatching {
            personMediator.finnEllerOpprettPerson(søknadsbehandlingOpprettetHendelse.ident)
        }.onFailure { e ->
            when (e is AdresseBeeskyttetPersonException || e is SkjermetPersonException) {
                true -> {
                    sendAvbrytBehandling( søknadsbehandlingOpprettetHendelse = søknadsbehandlingOpprettetHendelse)
                }
                else -> { throw e }
            }
        }.onSuccess { person ->
            val sakHistorikk =
                sakRepository.finn(søknadsbehandlingOpprettetHendelse.ident) ?: SakHistorikk(
                    person = person,
                )
            sakHistorikk.leggTilSak(sak)
            sakRepository.lagre(sakHistorikk)
        }
    }

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse) {
        sakRepository.hentSakHistorikk(meldekortbehandlingOpprettetHendelse.ident).also {
            it.knyttTilSak(meldekortbehandlingOpprettetHendelse)
            sakRepository.lagre(it)
        }
    }

    private fun  sendAvbrytBehandling(
        søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse
    ) {
        rapidsConnection.publish(
            key = søknadsbehandlingOpprettetHendelse.ident,
            message =  JsonMessage.newMessage(
                eventName = "avbryt_behandling",
                map = mapOf(
                    "behandlingId" to søknadsbehandlingOpprettetHendelse.behandlingId,
                    "søknadId" to søknadsbehandlingOpprettetHendelse.søknadId,
                    "ident" to søknadsbehandlingOpprettetHendelse.ident,
                )
            ).toJson()
        )
        logger.info { "Publiserte avbryt_behandling hendelse" }
    }
}
