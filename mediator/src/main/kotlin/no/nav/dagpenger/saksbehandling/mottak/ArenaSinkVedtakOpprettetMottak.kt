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
import no.nav.dagpenger.saksbehandling.db.person.PersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

const val VEDTAKSTATUS_IVERKSATT = "IVERK"

class ArenaSinkVedtakOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
    private val utsendingMediator: UtsendingMediator,
    private val sakMediator: SakMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "arenasink_vedtak_opprettet")
            }
            validate {
                it.requireKey(
                    "søknadId",
                    "sakId",
                    "vedtakId",
                    "vedtakstatus",
                    "rettighet",
                    "utfall",
                    "kilde",
                )
            }
        }
    }

    // Se https://nav-it.slack.com/archives/C063581H0PR/p1753450480334419
    private val skipBehandlinger =
        setOf(
            "01984174-1dd2-7ebc-86d1-c85702ffaaf4",
        ).map { UUID.fromString(it) }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["kilde"]["id"].asUUID()
        if (behandlingId in skipBehandlinger) {
            logg.info { "Skipper behandling med id $behandlingId" }
            return
        }
        val ident = personRepository.hentPersonForBehandlingId(behandlingId).ident
        val sakId = packet["sakId"].asText()
        val vedtakstatus = packet["vedtakstatus"].asText()
        val behandletHendelseId = packet["søknadId"].asText()

        withLoggingContext(
            "behandlingId" to "$behandlingId",
            "sakId" to sakId,
        ) {
            logg.info { "Mottok arenasink_vedtak_opprettet hendelse" }
            sikkerlogg.info { "Mottok arenasink_vedtak_opprettet hendelse ${packet.toJson()}" }

            if (vedtakstatus == VEDTAKSTATUS_IVERKSATT) {
                val vedtakFattetHendelse =
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        behandletHendelseId = behandletHendelseId,
                        behandletHendelseType = "Søknad",
                        ident = ident,
                        sak =
                            UtsendingSak(
                                id = sakId,
                                kontekst = "Arena",
                            ),
                    )
                sakMediator.oppdaterSakMedArenaSakId(vedtakFattetHendelse)
                utsendingMediator.startUtsendingForVedtakFattet(vedtakFattetHendelse)
            } else {
                logg.info { "Vedtakstatus fra Arena er $vedtakstatus. Gjør ingenting." }
            }
        }
    }
}
