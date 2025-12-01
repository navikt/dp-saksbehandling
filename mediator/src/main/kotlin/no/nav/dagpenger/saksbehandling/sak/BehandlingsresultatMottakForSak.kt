package no.nav.dagpenger.saksbehandling.sak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.mottak.uuidOrNull
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class BehandlingsresultatMottakForSak(
    rapidsConnection: RapidsConnection,
    private val sakRepository: SakRepository,
    private val sakMediator: SakMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behandlingsresultat")
                it.requireValue("behandletHendelse.type", "Søknad")
                it.requireKey("rettighetsperioder")
            }
            validate {
                it.requireKey("ident", "behandlingId", "behandletHendelse", "automatisk")
                it.interestedIn("basertPåBehandling")
            }
        }
    }

    init {
        logger.info { "Starter BehandlingsresultatMottakForSak" }
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingResultat = BehandlingResultat(packet)
        val behandlingId = behandlingResultat.behandlingId
        withLoggingContext("behandlingId" to "$behandlingId") {
            logger.info { "Mottok behandlingresultat hendelse i BehandlingsresultatMottakForSak" }

            if (vedtakSkalMeføreNyDpSak(behandlingResultat)) {
                val ident = packet["ident"].asText()
                val sakId = sakRepository.hentSakIdForBehandlingId(behandlingId).toString()
                val automatiskBehandlet = packet["automatisk"].asBoolean()
                val behandletHendelseId = packet["behandletHendelse"]["id"].asText()
                logger.info { "Vedtak skal tilhøre dp-sak " }
                sakMediator.merkSakenSomDpSak(
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = behandletHendelseId,
                        behandletHendelseType = behandlingResultat.behandletHendelseType,
                        ident = ident,
                        sak =
                            UtsendingSak(
                                id = sakId,
                                kontekst = "Dagpenger",
                            ),
                        automatiskBehandlet = automatiskBehandlet,
                    ),
                )
            }
        }
    }

    private fun vedtakSkalMeføreNyDpSak(behandlingResultat: BehandlingResultat): Boolean {
        return (behandlingResultat.basertPåBehandlingId == null && behandlingResultat.dagpengerInnvilget()).also {
            logger.info { "BehandlingsresultatMottakForSak med utfall: $it. Basert på $behandlingResultat" }
        }
    }
}

// todo flytt til en egen klasse
internal data class BehandlingResultat(
    val behandlingId: UUID,
    val basertPåBehandlingId: UUID? = null,
    val behandletHendelseType: String,
    val rettighetsperioder: List<Rettighetsperiode>,
) {
    constructor(packet: JsonMessage) : this(
        behandlingId = packet["behandlingId"].asUUID(),
        basertPåBehandlingId = packet["basertPåBehandling"].uuidOrNull(),
        behandletHendelseType = packet["behandletHendelse"]["type"].asText(),
        rettighetsperioder =
            packet["rettighetsperioder"].map {
                Rettighetsperiode(
                    harRett = it["harRett"].asBoolean(),
                )
            },
    )

    fun dagpengerInnvilget(): Boolean {
        return behandletHendelseType == "Søknad" &&
            rettighetsperioder.any { it.harRett }
    }

    data class Rettighetsperiode(
        val harRett: Boolean,
    )
}
