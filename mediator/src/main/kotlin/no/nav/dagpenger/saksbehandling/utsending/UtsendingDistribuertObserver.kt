package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class UtsendingDistribuertObserver(
    private val rapidsConnection: RapidsConnection,
) : UtsendingHendelseObserver {
    override fun onDistribuert(
        hendelse: DistribuertHendelse,
        utsending: Utsending,
    ) {
        val message =
            JsonMessage
                .newMessage(
                    eventName = "utsending_distribuert",
                    mapOf(
                        "behandlingId" to hendelse.behandlingId.toString(),
                        "utsendingId" to utsending.id.toString(),
                        "distribusjonId" to hendelse.distribusjonId,
                        "journalpostId" to hendelse.journalpostId,
                        "ident" to utsending.ident,
                        "type" to utsending.type.name,
                    ),
                ).toJson()

        logger.info {
            "Publiserer utsending_distribuert for behandlingId: ${hendelse.behandlingId}, distribusjonId: ${hendelse.distribusjonId}"
        }
        sikkerlogg.info { "Publiserer melding: $message" }

        rapidsConnection.publish(key = utsending.ident, message = message)
    }
}
