package no.nav.dagpenger.behandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.hendelser.mottak.SøknadMottak
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

internal class ApplicationBuilder(configuration: Map<String, String>) : RapidsConnection.StatusListener {

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(configuration))
            .withKtorModule {
                oppgaveApi()
            }.build()

    init {
        rapidsConnection.register(this)
        SøknadMottak(
            rapidsConnection = rapidsConnection,
        )
    }

    fun start() {
        rapidsConnection.start()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logger.info { "Starter appen ${Configuration.appName}" }
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        logger.info { "Skrur av applikasjonen" }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
