package no.nav.dagpenger.saksbehandling

import io.ktor.server.application.install
import mu.KotlinLogging
import no.dagpenger.saksbehandling.streams.kafka.KafkaStreamsPlugin
import no.dagpenger.saksbehandling.streams.kafka.kafkaStreams
import no.dagpenger.saksbehandling.streams.skjerming.skjermetPersonStatus
import no.nav.dagpenger.saksbehandling.api.config.apiConfig
import no.nav.dagpenger.saksbehandling.api.oppgaveApi
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.frist.settOppgaverKlarTilBehandling
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdHttpClient
import no.nav.dagpenger.saksbehandling.mottak.AvklaringIkkeRelevantMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingAvbruttMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilVedtakMottak
import no.nav.dagpenger.saksbehandling.mottak.VedtakFattetMottak
import no.nav.dagpenger.saksbehandling.pdl.PDLHttpKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingConsumer
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingHttpKlient
import no.nav.dagpenger.saksbehandling.statistikk.PostgresStatistikkTjeneste
import no.nav.dagpenger.saksbehandling.statistikk.statistikkApi
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingBehovLøsningMottak
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingMottak
import no.nav.dagpenger.saksbehandling.utsending.utsendingApi
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

    private val oppgaveMediator = OppgaveMediator(oppgaveRepository, skjermingHttpKlient)
    private val utsendingMediator = UtsendingMediator(utsendingRepository)
    private val skjermingConsumer = SkjermingConsumer(oppgaveRepository)

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(configuration))
            .withKtorModule {
                this.apiConfig()
                this.oppgaveApi(oppgaveMediator, pdlKlient, journalpostIdClient)
                this.utsendingApi(utsendingMediator)
                this.statistikkApi(PostgresStatistikkTjeneste(PostgresDataSourceBuilder.dataSource))
                this.install(KafkaStreamsPlugin) {
                    kafkaStreams =
                        kafkaStreams(Configuration.kafkaStreamProperties) {
                            skjermetPersonStatus(
                                Configuration.skjermingPersonStatusTopic,
                                skjermingConsumer::oppdaterSkjermetStatus,
                            )
                        }
                }
            }.build().also { rapidsConnection ->
                utsendingMediator.setRapidsConnection(rapidsConnection)
                VedtakFattetMottak(rapidsConnection, oppgaveMediator)
                BehandlingOpprettetMottak(rapidsConnection, oppgaveMediator, pdlKlient)
                BehandlingAvbruttMottak(rapidsConnection, oppgaveMediator)
                ForslagTilVedtakMottak(rapidsConnection, oppgaveMediator)
                UtsendingMottak(rapidsConnection, utsendingMediator)
                UtsendingBehovLøsningMottak(rapidsConnection, utsendingMediator)
                AvklaringIkkeRelevantMottak(rapidsConnection, oppgaveMediator)
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
