package no.nav.dagpenger.saksbehandling.identendring

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.saksbehandling.AlertManager.IdentendringForSak
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository

private val logger = KotlinLogging.logger {}

internal class IdentendringConsumer(
    private val sakRepository: SakRepository,
    private val rapidsConnection: RapidsConnection,
    registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
) {
    private val counter: Counter =
        Counter
            .builder()
            .name("dp_saksbehandling_identendring")
            .labelNames("utfall")
            .help("Antall identendringer fra PDL, fordelt på om personen har dagpengesak")
            .register(registry)
            .apply {
                // Seriene må finnes fra 0, ellers fanger ikke increase() første økning
                initLabelValues("treff")
                initLabelValues("ikke_treff")
            }

    fun håndter(historiskeIdenter: Set<String>) {
        val sakIder = historiskeIdenter.mapNotNull { sakRepository.finnSisteDagpengeSakId(it) }.toSet()

        if (sakIder.isEmpty()) {
            counter.labelValues("ikke_treff").inc()
            return
        }

        sakIder.forEach { sakId ->
            logger.info { "Identendring for person med dagpengesak $sakId. Sender varsel." }
            rapidsConnection.sendAlertTilRapid(IdentendringForSak(sakId), null)
        }
        counter.labelValues("treff").inc()
    }
}
