package no.nav.dagpenger.saksbehandling.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import java.time.Duration
import java.time.LocalDateTime

/**
 * All utboks-logikk samlet ett sted. Bestemmer hvilke [UtboksTilstand]-verdier som
 * gjelder ved lagring, publisering og opprydding, og delegerer persistens til
 * [UtboksRepository].
 *
 * Implementerer to seam:
 * - [Utboks]: sender-seam brukt av mediatorene for å enqueue meldinger i en delt transaksjon.
 * - [UtboksVedlikehold]: jobb-seam brukt av bakgrunnsjobbene for publisering og opprydding.
 *
 * Garantier ved publisering:
 * - Global FIFO (ORDER BY id i repository)
 * - Stopper ved første feil — retry ved neste poll
 *   (bevisst: key=fnr til én rapid-strøm, så feil er normalt globale og bør feile høyt)
 * - fnr (key) og meldingsinnhold logges kun til sikkerlogg (GDPR)
 *
 * Leveringssemantikk: **at-least-once.** [publiserVentende] gjør publish (steg 1) og
 * markering som SENDT (steg 2) i to separate operasjoner. Krasjer poden mellom dem,
 * re-publiseres meldingen ved neste poll. Utboks eliminerer altså *tapte* meldinger,
 * ikke *dupliserte* — konsumenter må derfor være idempotente.
 */
class PostgresRapidUtboks(
    private val repository: UtboksRepository,
    private val rapidsConnection: RapidsConnection,
    private val levetidSendte: Duration = Duration.ofDays(7),
    registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
) : Utboks,
    UtboksVedlikehold {
    private val logger = KotlinLogging.logger {}
    private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    private val nyeMeldingerCounter = registry.lagNyeMeldingerCounter()
    private val publiserteMeldingerCounter = registry.lagPubliserteMeldingerCounter()
    private val ventendeMeldingerGauge = registry.lagVentendeMeldingerGauge()

    override fun send(
        key: String,
        message: String,
        ctx: Transaksjonskontekst.Aktiv,
    ) {
        repository.lagre(key = key, message = message, tilstand = UtboksTilstand.PENDING.name, ctx = ctx)
        nyeMeldingerCounter.inc()
    }

    override fun publiserVentende() {
        val (meldinger, totaltAntall) = repository.hentOgTellMedTilstand(UtboksTilstand.PENDING.name)
        logger.info { "Fant $totaltAntall ventende utboks-meldinger (${meldinger.size} hentes nå)" }
        var antallPublisert = 0
        try {
            for (melding in meldinger) {
                try {
                    rapidsConnection.publish(melding.key, melding.message)
                    repository.oppdaterTilstand(melding.id, UtboksTilstand.SENDT.name)
                    publiserteMeldingerCounter.inc("success")
                    antallPublisert++
                    logger.info { "Publiserte utboks id=${melding.id}" }
                    // fnr (key) og message-innhold KUN til sikkerlogg (GDPR)
                    sikkerlogg.info { "Publiserte utboks id=${melding.id} key=${melding.key}" }
                } catch (e: Exception) {
                    publiserteMeldingerCounter.inc("failed")
                    logger.error(e) { "Feil ved publisering av utboks id=${melding.id} — stopper polling" }
                    break
                }
            }
        } finally {
            ventendeMeldingerGauge.set((totaltAntall - antallPublisert).toDouble())
        }
    }

    override fun slettGamleSendte(): Int {
        val cutoff = LocalDateTime.now().minus(levetidSendte)
        return repository.slettMedTilstandEldreEnn(UtboksTilstand.SENDT.name, cutoff).also {
            logger.info { "Slettet $it utgåtte utboks-meldinger (SENDT eldre enn $levetidSendte)" }
        }
    }

    private fun Counter.inc(status: String) = this.labelValues(status).inc()

    private fun PrometheusRegistry.lagNyeMeldingerCounter(): Counter =
        Counter
            .builder()
            .name("dp_saksbehandling_utboks_nye_meldinger_total")
            .help("Antall utboks-meldinger lagt til")
            .register(this)

    private fun PrometheusRegistry.lagPubliserteMeldingerCounter(): Counter =
        Counter
            .builder()
            .name("dp_saksbehandling_utboks_publiserte_meldinger_total")
            .help("Antall utboks-meldinger prosessert")
            .labelNames("status")
            .register(this)

    private fun PrometheusRegistry.lagVentendeMeldingerGauge(): Gauge =
        Gauge
            .builder()
            .name("dp_saksbehandling_utboks_ventende_meldinger")
            .help("Antall utboks-meldinger som venter på å bli publisert (PENDING)")
            .register(this)
}
