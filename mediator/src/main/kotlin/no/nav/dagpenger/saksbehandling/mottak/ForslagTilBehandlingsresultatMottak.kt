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
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse

internal class ForslagTilBehandlingsresultatMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val logger = KotlinLogging.logger {}

        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "forslag_til_behandlingsresultat")
                it.requireAny(key = "behandletHendelse.type", values = listOf("Søknad", "Meldekort", "Manuell"))
                it.requireKey("ident", "behandlingId")
                it.requireKey("opplysninger")
                it.requireKey("behandletHendelse")
                it.requireKey("rettighetsperioder")
            }
        }
    }

    init {
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
        withLoggingContext("Id" to "$behandletHendelseId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok forslag_til_behandlingsresultat hendelse" }
            val ident = packet["ident"].asText()
            runCatching {
                val emneknagger = EmneknaggBuilder(packet.toJson()).bygg()
                val forslagTilVedtakHendelse =
                    ForslagTilVedtakHendelse(
                        ident = ident,
                        behandletHendelseId = behandletHendelseId,
                        behandletHendelseType = packet["behandletHendelse"]["type"].asText(),
                        behandlingId = behandlingId,
                        emneknagger = emneknagger,
                    )
                sikkerlogg.info { "Mottok forslag_til_behandlingsresultat hendelse: $forslagTilVedtakHendelse" }
                oppgaveMediator.opprettEllerOppdaterOppgave(forslagTilVedtakHendelse)
            }.onFailure {
                logger.error(it) { "Feil ved håndtering av forslag_til_behandlingsresultat hendelse" }
            }
        }
    }
}
