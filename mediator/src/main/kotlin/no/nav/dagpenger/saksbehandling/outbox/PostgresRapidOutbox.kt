package no.nav.dagpenger.saksbehandling.outbox

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import java.time.Duration
import java.time.LocalDateTime

/**
 * All outbox-logikk samlet ett sted. Bestemmer hvilke [OutboxTilstand]-verdier som
 * gjelder ved lagring, publisering og opprydding, og delegerer persistens til
 * [OutboxRepository].
 *
 * Implementerer to seam:
 * - [Outbox]: sender-seam brukt av mediatorene for å enqueue meldinger i en delt transaksjon.
 * - [OutboxVedlikehold]: jobb-seam brukt av bakgrunnsjobbene for publisering og opprydding.
 *
 * Garantier ved publisering:
 * - Global FIFO (ORDER BY id i repository)
 * - Stopper ved første feil — retry ved neste poll
 * - fnr (key) og meldingsinnhold logges kun til sikkerlogg (GDPR)
 *
 * Leveringssemantikk: **at-least-once.** [publiserVentende] gjør publish (steg 1) og
 * markering som SENDT (steg 2) i to separate operasjoner. Krasjer poden mellom dem,
 * re-publiseres meldingen ved neste poll. Outbox eliminerer altså *tapte* meldinger,
 * ikke *dupliserte* — konsumenter må derfor være idempotente.
 */
class PostgresRapidOutbox(
    private val repository: OutboxRepository,
    private val rapidsConnection: RapidsConnection,
    private val levetidSendte: Duration = Duration.ofDays(7),
) : Outbox,
    OutboxVedlikehold {
    private val logger = KotlinLogging.logger {}
    private val sikkerlogg = KotlinLogging.logger("tjenestekall")

    override fun send(
        key: String,
        message: String,
        ctx: Transaksjonskontekst.Aktiv,
    ) = repository.lagre(key = key, message = message, tilstand = OutboxTilstand.PENDING.name, ctx = ctx)

    override fun publiserVentende() {
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

    override fun slettGamleSendte(): Int {
        val cutoff = LocalDateTime.now().minus(levetidSendte)
        return repository.slettMedTilstandEldreEnn(OutboxTilstand.SENDT.name, cutoff).also {
            logger.info { "Slettet $it utgåtte outbox-records (SENDT eldre enn $levetidSendte)" }
        }
    }
}
