package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository

class UtsendingAlarmJob(
    private val rapidsConnection: RapidsConnection,
    private val utsendingRepository: UtsendingRepository,
) {
    fun sjekkVentendeTilstander() {
        utsendingRepository.hentVentendeUtsendinger().forEach {
            if (it.venterPåUtsending()) {
                rapidsConnection.publish(
                    UtsendingAlarm(
                        aktørId = it.aktørId,
                        journalpostId = it.journalpostId,
                        oppgaveId = it.oppgaveId,
                        opprettet = it.opprettet,
                        tilstand = it.tilstand,
                    ),
                )
            }
        }
    }




}