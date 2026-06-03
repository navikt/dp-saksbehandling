package no.nav.dagpenger.saksbehandling.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst

/**
 * Synkron utboks-implementasjon for tester — sender direkte til [rapidsConnection]
 * i stedet for å skrive til DB. Brukes med TestRapid slik at mediator-tester
 * ikke trenger å endre assertions.
 */
class TestUtboks(
    private val rapidsConnection: RapidsConnection,
) : Utboks {
    override fun send(
        key: String,
        message: String,
        ctx: Transaksjonskontekst.Aktiv,
    ) {
        rapidsConnection.publish(key = key, message = message)
    }
}
