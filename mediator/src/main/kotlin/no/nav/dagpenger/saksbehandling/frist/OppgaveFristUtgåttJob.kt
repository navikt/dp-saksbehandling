package no.nav.dagpenger.saksbehandling.frist

import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import java.time.LocalDate
import java.util.UUID
import kotlin.concurrent.fixedRateTimer

private val logger = KotlinLogging.logger {}

fun settOppgaverKlarTilBehandling() {
    val vaktmester = no.nav.dagpenger.saksbehandling.frist.OppgaveFristUtgåttJob(dataSource)

    fixedRateTimer(
        name = "",
        daemon = true,
        initialDelay = 1.Minutt,
        period = 15.Minutt,
        action = {
            try {
                vaktmester.settOppgaverMedUtgåttFristTilKlarTilBehandling()
            } catch (e: Exception) {
                logger.error { "Sletterutine feilet: $e" }
            }
        },
    )
}

private val Int.Minutt get() = this * 1000L * 60L

fun settOppgaverMedUtgåttFristTilKlarTilBehandling(frist: LocalDate = LocalDate.now()) {
    sessionOf(dataSource).use { session ->
        val utgåtteOppgaver: List<UUID> =
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

        session.batchPreparedStatement(
            //language=PostgreSQL
            statement =
            """
                    UPDATE oppgave_v1
                    SET    tilstand = '${Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING.name}',
                           utsatt_til = null
                    WHERE id = ?
                    """.trimIndent(),
            params = listOf(utgåtteOppgaver),
        )
    }
}
