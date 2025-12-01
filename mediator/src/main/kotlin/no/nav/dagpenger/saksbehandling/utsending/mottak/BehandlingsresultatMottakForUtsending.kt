package no.nav.dagpenger.saksbehandling.utsending.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.mottak.AbstractBehandlingResultatMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingResultat
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class BehandlingsresultatMottakForUtsending(
    rapidsConnection: RapidsConnection,
    private val utsendingMediator: UtsendingMediator,
    private val sakRepository: SakRepository,
) : AbstractBehandlingResultatMottak(rapidsConnection) {
    override fun requiredBehandletHendelseType(): List<String> = listOf("Søknad", "Manuell", "Meldekort")

    override val mottakNavn: String = "BehandlingsresultatMottakForUtsending"

    override fun håndter(
        behandlingResultat: BehandlingResultat,
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val dagpengerSakId by lazy {
            try {
                sakRepository.hentDagpengerSakIdForBehandlingId(behandlingResultat.behandlingId)
            } catch (e: Exception) {
                null
            }
        }

        val skalStarteUtsending =
            (behandlingResultat.nyDagpengerettInnvilget() || dagpengerSakId != null).also {
                logger.info {
                    "BehandlingsresultatMottakForUtsending med utfall: $it. Basert på $behandlingResultat".also { msg ->
                        if (!behandlingResultat.nyDagpengerettInnvilget()) {
                            msg.plus("DagpengerSakId: $dagpengerSakId")
                        }
                    }
                }
            }

        if (skalStarteUtsending) {
            val sakId = sakRepository.hentSakIdForBehandlingId(behandlingResultat.behandlingId).toString()
            val vedtakFattetHendelse =
                packet.vedtakFattetHendelse(
                    sak =
                        UtsendingSak(
                            id = sakId,
                            kontekst = "Dagpenger",
                        ),
                    behandlingResultat = behandlingResultat,
                )

            utsendingMediator.startUtsendingForVedtakFattet(
                vedtakFattetHendelse = vedtakFattetHendelse,
            )

            context.publish(
                key = packet["ident"].asText(),
                message =
                    JsonMessage.newMessage(
                        map =
                            VedtakUtenforArena(
                                behandlingId = behandlingResultat.behandlingId,
                                søknadId = behandlingResultat.behandletHendelseId,
                                ident = packet["ident"].asText(),
                                sakId = sakId,
                            ).toMap(),
                    ).toJson(),
            )
        }
    }

    private data class VedtakUtenforArena(
        val behandlingId: UUID,
        val søknadId: String,
        val ident: String,
        val sakId: String,
    ) {
        fun toMap(): Map<String, String> =
            mapOf(
                "@event_name" to "vedtak_fattet_utenfor_arena",
                "behandlingId" to behandlingId.toString(),
                "søknadId" to søknadId,
                "ident" to ident,
                "sakId" to sakId,
            )
    }
}
