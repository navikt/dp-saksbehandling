package no.nav.dagpenger.behandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.api.oppgaveApi
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

internal class ApplicationBuilder(configuration: Map<String, String>) : RapidsConnection.StatusListener {
    private val mediator =
        Mediator()
    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(configuration))
            .withKtorModule {
                this.oppgaveApi(mediator)
            }.build()

    init {
        rapidsConnection.register(this)
    }

    fun start() {
        rapidsConnection.start()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logger.info { "Starter appen ${Configuration.APP_NAME}" }
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        logger.info { "Skrur av applikasjonen" }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
