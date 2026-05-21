package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal abstract class AbstractBehandlingsresultatMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        logger.info { "Starter $mottakNavn" }
        River(rapidsConnection).apply(rapidFilter()).register(this)
    }

    fun rapidFilter(): River.() -> Unit =
        {
            precondition {
                it.requireAny("@event_name", requiredEventNames())
                val typer = requiredBehandletHendelseType()
                if (typer.isNotEmpty()) {
                    it.requireAny("behandletHendelse.type", typer)
                }
            }
            validate {
                it.requireKey("ident", "behandlingId", "behandletHendelse", "automatisk")
                it.interestedIn("rettighetsperioder")
                // TODO sjekk navngiving
                it.interestedIn("saksbehandlerIdent")
                it.interestedIn("beslutterIdent")
                it.valideringsregler()
                it.interestedIn("basertPå")
            }
        }

    protected open fun requiredEventNames(): List<String> = listOf("behandlingsresultat")

    protected open fun JsonMessage.valideringsregler() {
    }

    protected open fun requiredBehandletHendelseType(): List<String> = emptyList()

    protected abstract val mottakNavn: String

    protected abstract fun håndter(
        behandlingsresultat: Behandlingsresultat,
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    )

    protected fun JsonMessage.vedtakFattetHendelse(
        sak: UtsendingSak?,
        behandlingsresultat: Behandlingsresultat,
    ): VedtakFattetHendelse {
        val ident = this["ident"].asString()

        return VedtakFattetHendelse(
            behandlingId = behandlingsresultat.behandlingId,
            behandletHendelseId = behandlingsresultat.behandletHendelseId,
            behandletHendelseType = behandlingsresultat.behandletHendelseType,
            ident = ident,
            sak = sak,
            automatiskBehandlet = behandlingsresultat.automatiskBehandlet,
            saksbehandlerIdent = behandlingsresultat.saksbehandlerIdent,
            beslutterIdent = behandlingsresultat.beslutterIdent,
        )
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingsresultat = Behandlingsresultat(packet)
        withLoggingContext(
            "behandletHendelseId" to behandlingsresultat.behandletHendelseId,
            "behandlingId" to "${behandlingsresultat.behandlingId}",
        ) {
            logger.info { "Mottok behandlingsresultat hendelse i $mottakNavn" }
            håndter(behandlingsresultat, packet, context, metadata, meterRegistry)
        }
    }
}

// todo flytt til en egen klasse
internal data class Behandlingsresultat(
    val behandlingId: UUID,
    val basertPåBehandlingId: UUID? = null,
    val behandletHendelseType: String,
    val behandletHendelseId: String,
    val rettighetsperioder: List<Rettighetsperiode>,
    val automatiskBehandlet: Boolean,
    val saksbehandlerIdent: String?,
    val beslutterIdent: String?,
) {
    constructor(packet: JsonMessage) : this(
        behandlingId = packet["behandlingId"].asUUID(),
        basertPåBehandlingId = packet["basertPå"].uuidOrNull(),
        behandletHendelseType = packet["behandletHendelse"]["type"].asString(),
        behandletHendelseId = packet["behandletHendelse"]["id"].asString(),
        automatiskBehandlet = packet["automatisk"].asBoolean(),
        saksbehandlerIdent = packet["saksbehandlerIdent"].asString(),
        beslutterIdent = packet["beslutterIdent"].asString(),
        rettighetsperioder =
            packet["rettighetsperioder"]
                .takeIf { it.isArray }
                ?.values()
                ?.map {
                    Rettighetsperiode(
                        harRett = it["harRett"].asBoolean(),
                    )
                } ?: emptyList(),
    )

    private fun dagpengerInnvilget(): Boolean =
        behandletHendelseType == "Søknad" &&
            rettighetsperioder.any { it.harRett }

    fun nyDagpengerettInnvilget(): Boolean = basertPåBehandlingId == null && dagpengerInnvilget()

    data class Rettighetsperiode(
        val harRett: Boolean,
    )
}
