package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.api.config.apiConfig
import no.nav.dagpenger.saksbehandling.api.oppgaveApi
import no.nav.dagpenger.saksbehandling.api.utsendingApi
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.saksbehandling.db.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.frist.settOppgaverKlarTilBehandling
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdHttpClient
import no.nav.dagpenger.saksbehandling.mottak.BehandlingAvbruttMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.BehovLøsningMottak
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilVedtakMottak
import no.nav.dagpenger.saksbehandling.mottak.UtsendingMottak
import no.nav.dagpenger.saksbehandling.mottak.VedtakFattetMottak
import no.nav.dagpenger.saksbehandling.pdl.PDLHttpKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingHttpKlient
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

internal class ApplicationBuilder(configuration: Map<String, String>) : RapidsConnection.StatusListener {
    private val oppgaveRepository = PostgresOppgaveRepository(PostgresDataSourceBuilder.dataSource)
    private val utsendingRepository = PostgresUtsendingRepository(PostgresDataSourceBuilder.dataSource)
    private val skjermingHttpKlient =
        SkjermingHttpKlient(
            skjermingApiUrl = Configuration.skjermingApiUrl,
            tokenProvider = Configuration.skjermingTokenProvider,
        )
    private val pdlKlient =
        PDLHttpKlient(
            url = Configuration.pdlUrl,
            tokenSupplier = Configuration.pdlTokenProvider,
        )

    private val journalpostIdClient =
        JournalpostIdHttpClient(
            journalpostIdApiUrl = Configuration.journalpostIdApiUrl,
            tokenProvider = Configuration.journalpostTokenProvider,
        )

    private val oppgaveMediator = OppgaveMediator(oppgaveRepository)
    private val utsendingMediator = UtsendingMediator(utsendingRepository)

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(configuration))
            .withKtorModule {
                this.apiConfig()
                this.oppgaveApi(oppgaveMediator, pdlKlient, journalpostIdClient)
                this.utsendingApi(utsendingMediator)
            }.build().also { rapidsConnection ->
                utsendingMediator.setRapidsConnection(rapidsConnection)
                VedtakFattetMottak(rapidsConnection, oppgaveMediator)
                BehandlingOpprettetMottak(rapidsConnection, oppgaveMediator, skjermingHttpKlient, pdlKlient)
                BehandlingAvbruttMottak(rapidsConnection, oppgaveMediator)
                ForslagTilVedtakMottak(rapidsConnection, oppgaveMediator)
                UtsendingMottak(rapidsConnection, utsendingMediator)
                BehovLøsningMottak(rapidsConnection, utsendingMediator)
            }

    init {
        rapidsConnection.register(this)
    }

    fun start() {
        settOppgaverKlarTilBehandling()
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
