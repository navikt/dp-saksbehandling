package no.nav.dagpenger.saksbehandling.frist

import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer

private val logger = KotlinLogging.logger {}

fun settOppgaverKlarTilBehandling() {
    val date = Date.from(Instant.now().atZone(ZoneId.of("Europe/Oslo")).toInstant())

    fixedRateTimer(
        name = "",
        daemon = true,
        startAt = date,
        period = 1.Dag,
        action = {
            try {
                logger.info { "Starter settOppgaverMedUtgåttFristTilKlarTilBehandling jobb" }
                settOppgaverMedUtgåttFristTilKlarTilBehandling(
                    PostgresDataSourceBuilder.dataSource,
                    frist = LocalDate.now(),
                )
            } catch (e: Exception) {
                logger.error(e) { "SettOppgaverMedUtgåttFristTilKlarTilBehandling feilet: ${e.message} " }
            }
        },
    )
}

private val Int.Dag get() = this * 1000L * 60L * 60L * 24L

fun settOppgaverMedUtgåttFristTilKlarTilBehandling(
    dataSource: DataSource,
    frist: LocalDate = LocalDate.now(),
) {
    sessionOf(dataSource).use { session ->
        val utgåtteOppgaver: List<List<UUID>> =
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT id
                        FROM oppgave_v1
                        WHERE tilstand = :tilstand
                            AND utsatt_til < :frist
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "frist" to frist,
                            "tilstand" to PAA_VENT.name,
                        ),
                ).map { row ->
                    row.uuid("id")
                }.asList,
            )
                .also {
                    logger.info { "${it.size} oppgaver skal settes tilbake til KLAR_TIL_BEHANDLING: $it" }
                }
                .map { listOf(it) }

        session.batchPreparedStatement(
            //language=PostgreSQL
            statement =
                """
                UPDATE oppgave_v1
                SET    tilstand = '${Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING.name}',
                       utsatt_til = null
                WHERE id = ?
                """.trimIndent(),
            params = utgåtteOppgaver,
        ).also {
            logger.info { "Oppgaver oppdatert: ${it.sum()}" }
        }
    }
}
