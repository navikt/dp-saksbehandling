package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Timer
import java.util.UUID
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer

private val logger = KotlinLogging.logger {}

internal class SletteGamleOppgaverJob(
    private val rapidsConnection: RapidsConnection,
    private val gamleOppgaverRepository: GamleOppgaverRepository,
) {
    private val Int.Dag get() = this * 1000L * 60L * 60L * 24L

    fun startJob(): Timer {
        return fixedRateTimer(
            name = "SletteGamleOppgaverJob",
            daemon = true,
            startAt = Date.from(Instant.now().atZone(ZoneId.of("Europe/Oslo")).toInstant()),
            period = 1.Dag,
            action = {
                try {
                    logger.info { "Starter SletteGamleOppgaverJob" }
                    SletteGamleOppgaverJob(
                        rapidsConnection = rapidsConnection,
                        gamleOppgaverRepository = gamleOppgaverRepository,
                    ).avbrytGamleOppgaver()
                } catch (e: Exception) {
                    logger.error(e) { "SletteGamleOppgaverJob feilet: ${e.message} " }
                }
            },
        )
    }

    fun finnGamleOppgaver(eldreEnn: Int): List<GamleOppgaver> {
        return gamleOppgaverRepository.finnGamleOppgaver(eldreEnn).also {
            logger.info { "Fant ${it.size} gamle oppgaver: $it" }
        }
    }

    fun avbrytGamleOppgaver(eldreEnn: Int = 30) {
        finnGamleOppgaver(eldreEnn).forEach { oppgave ->
            rapidsConnection.publish(
                key = oppgave.ident,
                JsonMessage.newMessage(
                    eventName = "avbryt_behandling",
                    map =
                        mapOf(
                            "behandlingId" to oppgave.behandlingId,
                            "søknadId" to oppgave.soknadId,
                            "ident" to oppgave.ident,
                        ),
                ).toJson(),
            )
        }
    }
}

internal class GamleOppgaverRepository(private val ds: DataSource) {
    fun finnGamleOppgaver(intervallAntallDager: Int): List<GamleOppgaver> {
        return sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT
                          person_v1.ident,
                          oppgave_v1.behandling_id,
                          hendelse_v1.hendelse_data->>'søknadId' AS søknadId
                      FROM
                          oppgave_v1
                              JOIN
                          behandling_v1 ON oppgave_v1.behandling_id = behandling_v1.id
                              JOIN
                          person_v1 ON behandling_v1.person_id = person_v1.id
                              JOIN
                          hendelse_v1 ON hendelse_v1.behandling_id = behandling_v1.id
                      WHERE
                          oppgave_v1.endret_tidspunkt < NOW() - INTERVAL '$intervallAntallDager days'
                        AND oppgave_v1.tilstand NOT IN ('FERDIG_BEHANDLET', 'BEHANDLES_I_ARENA')
                    """.trimIndent(),
                ).map { row ->
                    GamleOppgaver(
                        ident = row.string("ident"),
                        soknadId = row.uuid("søknadId"),
                        behandlingId = row.uuid("behandling_id"),
                    )
                }.asList,
            )
        }
    }
}

data class GamleOppgaver(
    val ident: String,
    val soknadId: UUID,
    val behandlingId: UUID,
)
