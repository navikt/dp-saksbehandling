package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

internal class BehandlingsResultatMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behandlingsresultat")
                it.requireAny(key = "behandletHendelse.type", values = listOf("Søknad", "Meldekort", "Manuell"))
            }
            validate {
                it.requireKey("ident", "behandlingId", "behandletHendelse", "automatisk", "opplysninger")
            }
        }
    }

    init {
        logger.info { " Starter BehandlingsResultatMottak" }
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandletHendelseId = packet["behandletHendelse"]["id"].asText()
        val behandlingId = packet["behandlingId"].asUUID()

        withLoggingContext("behandletHendelseId" to "$behandletHendelseId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandlingsresultat hendelse" }

            val sak =
                packet.sak().onFailure {
                    logger.error(it) {
                        "Kunne ikke finne fagsakId for behandlingId=$behandlingId. Sjekke sikkerlogg for pakke prosessert"
                    }
                    sikkerlogger.error(it) { "Kunne ikke finne fagsakId for behandlingsResultat:${packet.toJson()}" }
                }.getOrThrow()

            oppgaveMediator.hentOppgaveIdFor(behandlingId)?.let {
                oppgaveMediator.ferdigstillOppgave(
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = behandletHendelseId,
                        behandletHendelseType = packet["behandletHendelse"]["type"].asText(),
                        ident = packet["ident"].asText(),
                        sak = sak,
                        automatiskBehandlet = packet["automatisk"].asBoolean(),
                    ),
                )
            }
        }
    }

    // ugh...
    // todo: Sjekk denne
    private fun JsonMessage.sak(): Result<UtsendingSak> {
        return runCatching {
            this["opplysninger"].single {
                it["opplysningTypeId"].asText() == "0194881f-9462-78af-8977-46092bb030eb"
            }.let { opplysning ->
                opplysning["perioder"].single {
                    it["status"].asText() == "Ny"
                }.let { periode ->
                    UtsendingSak(
                        id = periode["verdi"]["verdi"].asText(),
                    )
                }
            }
        }
    }
}
