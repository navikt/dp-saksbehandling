package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.OppgaveAlertManager
import no.nav.dagpenger.saksbehandling.OppgaveAlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Avbrutt
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import javax.sql.DataSource

internal class UtsendingAlarmJob(
    private val rapidsConnection: RapidsConnection,
    private val utsendingAlarmRepository: UtsendingAlarmRepository,
) {
    fun sjekkVentendeTilstander() {
        utsendingAlarmRepository.hentVentendeUtsendinger(24)
            .forEach {
                rapidsConnection.sendAlertTilRapid(
                    it,
                    it.feilMelding,
                )
            }
    }
}

internal class UtsendingAlarmRepository(private val ds: DataSource) {
    fun hentVentendeUtsendinger(intervallAntallTimer: Int): List<OppgaveAlertManager.UtsendingIkkeFullført> {
        return sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  *
                        FROM    utsending_v1
                        WHERE   tilstand != :distribuert 
                        AND     tilstand != :avbrutt
                        AND     endret_tidspunkt < NOW() - INTERVAL '$intervallAntallTimer hours'
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "distribuert" to Distribuert.name,
                            "avbrutt" to Avbrutt.name,
                        ),
                ).map { row ->
                    OppgaveAlertManager.UtsendingIkkeFullført(
                        utsendingId = row.uuid("id"),
                        tilstand = row.string("tilstand"),
                        sistEndret = row.localDateTime("endret_tidspunkt"),
                    )
                }.asList,
            )
        }
    }
}
