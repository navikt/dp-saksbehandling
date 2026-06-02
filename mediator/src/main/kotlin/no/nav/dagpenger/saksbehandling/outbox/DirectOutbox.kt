package no.nav.dagpenger.saksbehandling.outbox

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst

/**
 * Synkron outbox-implementasjon for tester — sender direkte til [rapidsConnection]
 * i stedet for å skrive til DB. Brukes med TestRapid slik at mediator-tester
 * ikke trenger å endre assertions.
 */
class DirectOutbox(
    private val rapidsConnection: RapidsConnection,
) : Outbox {
    override fun send(
        key: String,
        message: String,
        ctx: Transaksjonskontekst.Aktiv,
    ) {
        rapidsConnection.publish(key = key, message = message)
    }
}
