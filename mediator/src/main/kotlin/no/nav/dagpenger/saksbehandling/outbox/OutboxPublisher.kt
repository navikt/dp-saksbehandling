package no.nav.dagpenger.saksbehandling.outbox

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Leser PENDING-rader fra outbox via [OutboxRepository] og publiserer dem til Rapids & Rivers.
 *
 * Garantier:
 * - Global FIFO (ORDER BY id i repository)
 * - Stopper ved første feil — retry ved neste poll
 * - fnr (key) og meldingsinnhold logges kun til sikkerlogg (GDPR)
 */
class OutboxPublisher(
    private val repository: OutboxRepository,
    private val rapidsConnection: RapidsConnection,
) {
    private val logger = KotlinLogging.logger {}
    private val sikkerlogg = KotlinLogging.logger("tjenestekall")

    fun publiser() {
        for (record in repository.hentMedTilstand(OutboxTilstand.PENDING.name)) {
            try {
                rapidsConnection.publish(record.key, record.message)
                repository.oppdaterTilstand(record.id, OutboxTilstand.SENDT.name)
                logger.info { "Publiserte outbox id=${record.id}" }
                // fnr (key) og message-innhold KUN til sikkerlogg (GDPR)
                sikkerlogg.info { "Publiserte outbox id=${record.id} key=${record.key}" }
            } catch (e: Exception) {
                logger.error(e) { "Feil ved publisering av outbox id=${record.id} — stopper polling" }
                break
            }
        }
    }
}
