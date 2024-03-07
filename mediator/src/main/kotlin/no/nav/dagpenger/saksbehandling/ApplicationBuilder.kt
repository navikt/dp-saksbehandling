package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.api.oppgaveApi
import no.nav.dagpenger.saksbehandling.db.InMemoryRepository
import no.nav.dagpenger.saksbehandling.maskinell.BehandlingKlient
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

internal class ApplicationBuilder(configuration: Map<String, String>) : RapidsConnection.StatusListener {
    private val inMemoryRepository = InMemoryRepository()
    private val behandlingKlient: BehandlingKlient =
        BehandlingKlient(
            behandlingUrl = Configuration.behandlingUrl,
            behandlingScope = Configuration.behandlingScope,
            tokenProvider = Configuration.tilOboToken,
        )
    private val mediator = Mediator(inMemoryRepository, inMemoryRepository, behandlingKlient)

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(configuration))
            .withKtorModule {
                this.oppgaveApi(mediator)
            }.build().also { rapidsConnection ->
                BehandlingOpprettetMottak(rapidsConnection, mediator)
            }

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
