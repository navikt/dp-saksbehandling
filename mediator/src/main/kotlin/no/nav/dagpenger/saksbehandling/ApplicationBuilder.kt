package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.api.oppgaveApi
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.saksbehandling.db.PostgresRepository
import no.nav.dagpenger.saksbehandling.mottak.BehandlingAvbruttMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilVedtakMottak
import no.nav.dagpenger.saksbehandling.mottak.VedtakFattetMottak
import no.nav.dagpenger.saksbehandling.pdl.PDLHttpKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingHttpKlient
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

internal class ApplicationBuilder(configuration: Map<String, String>) : RapidsConnection.StatusListener {
    private val repository = PostgresRepository(PostgresDataSourceBuilder.dataSource)
    private val skjermingHttpKlient = SkjermingHttpKlient(
        skjermingApiUrl = Configuration.skjermingApiUrl,
        tokenProvider = Configuration.skjermingTokenProvider,
    )
    private val pdlKlient = PDLHttpKlient(
        url = Configuration.pdlUrl,
        tokenSupplier = Configuration.pdlTokenProvider,
    )

    private val mediator = Mediator(repository)

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(configuration))
            .withKtorModule {
                this.oppgaveApi(mediator, pdlKlient)
            }.build().also { rapidsConnection ->
                VedtakFattetMottak(rapidsConnection, mediator)
                BehandlingOpprettetMottak(rapidsConnection, mediator, skjermingHttpKlient, pdlKlient)
                BehandlingAvbruttMottak(rapidsConnection, mediator)
                ForslagTilVedtakMottak(rapidsConnection, mediator)
            }

    init {
        rapidsConnection.register(this)
    }

    fun start() {
        rapidsConnection.start()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        runMigration()
        logger.info { "Starter appen ${Configuration.APP_NAME}" }
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        logger.info { "Skrur av applikasjonen" }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
