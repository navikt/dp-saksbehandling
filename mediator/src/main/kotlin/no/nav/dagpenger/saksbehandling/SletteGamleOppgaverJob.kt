package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KLogger
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.job.Job
import java.util.UUID
import javax.sql.DataSource

internal class SletteGamleOppgaverJob(
    private val rapidsConnection: RapidsConnection,
    private val gamleOppgaverRepository: GamleOppgaverRepository,
) : Job() {
    companion object {
        private const val ELDRE_ENN = 21
    }

    override val jobName: String = "SletteGamleOppgaverJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        gamleOppgaverRepository.finnGamleOppgaver(ELDRE_ENN).also {
            logger.info { "Fant ${it.size} gamle oppgaver: $it" }
        }.forEach { oppgave ->
            rapidsConnection.publish(
                key = oppgave.ident,
                message =
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
                          oppgave_v1.opprettet < NOW() - INTERVAL '$intervallAntallDager days'
                        AND oppgave_v1.tilstand  IN ('OPPRETTET', 'KLAR_TIL_BEHANDLING', 'PAA_VENT')
                        AND oppgave_v1.saksbehandler_ident IS NULL
                    """.trimIndent(),
                ).map { row ->
                    GamleOppgaver(
                        ident = row.string("ident"),
                        soknadId = row.string("søknadId").let { UUID.fromString(it) },
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
