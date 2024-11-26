package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.saksbehandling.OppgaveAlertManager
import no.nav.dagpenger.saksbehandling.OppgaveAlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository

class UtsendingAlarmJob(
    private val rapidsConnection: RapidsConnection,
    private val utsendingRepository: UtsendingRepository,
) {
    fun sjekkVentendeTilstander() {
        utsendingRepository.hentVentendeUtsendinger()
            .map {
                val utsending = it.first
                OppgaveAlertManager.UtsendingIkkeFullført(
                    utsendingId = utsending.id,
                    tilstand = utsending.tilstand(),
                    sistEndret = it.second,
                )
            }
            .forEach {
                rapidsConnection.sendAlertTilRapid(
                    it,
                    it.feilMelding,
                )
            }
    }
}
