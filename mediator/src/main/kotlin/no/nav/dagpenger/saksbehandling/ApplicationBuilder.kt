package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.KafkaRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.install
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.adressebeskyttelse.AdressebeskyttelseConsumer
import no.nav.dagpenger.saksbehandling.api.OppgaveDTOMapper
import no.nav.dagpenger.saksbehandling.api.OppgaveHistorikkDTOMapper
import no.nav.dagpenger.saksbehandling.api.allApis
import no.nav.dagpenger.saksbehandling.api.statusPages
import no.nav.dagpenger.saksbehandling.behandling.BehandlingHttpKlient
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.frist.settOppgaverKlarTilBehandlingEllerUnderBehandling
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdHttpClient
import no.nav.dagpenger.saksbehandling.metrikker.startMetrikkJob
import no.nav.dagpenger.saksbehandling.mottak.ArenaSinkVedtakOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.AvklaringIkkeRelevantMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingAvbruttMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingLåstMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpplåstMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilVedtakMottak
import no.nav.dagpenger.saksbehandling.mottak.MeldingOmVedtakProdusentBehovløser
import no.nav.dagpenger.saksbehandling.mottak.VedtakFattetMottak
import no.nav.dagpenger.saksbehandling.pdl.PDLHttpKlient
import no.nav.dagpenger.saksbehandling.saksbehandler.CachedSaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslagImpl
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingConsumer
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingHttpKlient
import no.nav.dagpenger.saksbehandling.statistikk.PostgresStatistikkTjeneste
import no.nav.dagpenger.saksbehandling.streams.kafka.KafkaStreamsPlugin
import no.nav.dagpenger.saksbehandling.streams.kafka.kafkaStreams
import no.nav.dagpenger.saksbehandling.streams.leesah.adressebeskyttetStream
import no.nav.dagpenger.saksbehandling.streams.skjerming.skjermetPersonStatus
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingBehovLøsningMottak
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingMottak
import no.nav.helse.rapids_rivers.RapidApplication

internal class ApplicationBuilder(configuration: Map<String, String>) : RapidsConnection.StatusListener {
    private val oppgaveRepository = PostgresOppgaveRepository(PostgresDataSourceBuilder.dataSource)
    private val utsendingRepository = PostgresUtsendingRepository(PostgresDataSourceBuilder.dataSource)
    private val skjermingKlient =
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
    private val behandlingKlient =
        BehandlingHttpKlient(
            dpBehandlingApiUrl = Configuration.dbBehandlingApiUrl,
            tokenProvider = Configuration.dpBehandlingOboExchanger,
        )
    private val utsendingMediator = UtsendingMediator(utsendingRepository)
    private val oppgaveMediator =
        OppgaveMediator(
            repository = oppgaveRepository,
            skjermingKlient = skjermingKlient,
            pdlKlient = pdlKlient,
            behandlingKlient = behandlingKlient,
            utsendingMediator = utsendingMediator,
        )
    private val skjermingConsumer = SkjermingConsumer(oppgaveRepository)
    private val adressebeskyttelseConsumer = AdressebeskyttelseConsumer(oppgaveRepository, pdlKlient)
    private val saksbehandlerOppslag =
        CachedSaksbehandlerOppslag(SaksbehandlerOppslagImpl(tokenProvider = Configuration.entraTokenProvider))
    private val oppgaveDTOMapper =
        OppgaveDTOMapper(
            pdlKlient,
            journalpostIdClient,
            saksbehandlerOppslag,
            OppgaveHistorikkDTOMapper(oppgaveRepository, saksbehandlerOppslag),
        )

    private val rapidsConnection: RapidsConnection =
        RapidApplication.create(
            env = configuration,
            objectMapper = objectMapper,
            builder = {
                withKtorModule {
                    allApis(
                        oppgaveMediator,
                        oppgaveDTOMapper,
                        PostgresStatistikkTjeneste(PostgresDataSourceBuilder.dataSource),
                    )
                    withStatusPagesConfig { statusPages() }
                    this.install(KafkaStreamsPlugin) {
                        kafkaStreams =
                            kafkaStreams(Configuration.kafkaStreamProperties) {
                                skjermetPersonStatus(
                                    Configuration.skjermingPersonStatusTopic,
                                    skjermingConsumer::oppdaterSkjermetStatus,
                                )
                                adressebeskyttetStream(
                                    Configuration.leesahTopic,
                                    adressebeskyttelseConsumer::oppdaterAdressebeskyttelseStatus,
                                )
                            }
                    }
                }
            },
        ) { _: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>, _: KafkaRapid ->
        }.also { rapidsConnection ->
            utsendingMediator.setRapidsConnection(rapidsConnection)
            oppgaveMediator.setRapidsConnection(rapidsConnection)
            VedtakFattetMottak(rapidsConnection, oppgaveMediator, utsendingMediator)
            BehandlingOpprettetMottak(rapidsConnection, oppgaveMediator, pdlKlient, skjermingKlient)
            BehandlingAvbruttMottak(rapidsConnection, oppgaveMediator)
            ForslagTilVedtakMottak(rapidsConnection, oppgaveMediator)
            UtsendingMottak(rapidsConnection, utsendingMediator)
            UtsendingBehovLøsningMottak(rapidsConnection, utsendingMediator)
            AvklaringIkkeRelevantMottak(rapidsConnection, oppgaveMediator)
            ArenaSinkVedtakOpprettetMottak(
                rapidsConnection,
                oppgaveRepository,
                utsendingMediator,
            )
            MeldingOmVedtakProdusentBehovløser(rapidsConnection, utsendingMediator)
            BehandlingLåstMottak(rapidsConnection, oppgaveMediator)
            BehandlingOpplåstMottak(rapidsConnection, oppgaveMediator)
        }

    init {
        rapidsConnection.register(this)
    }

    fun start() {
        settOppgaverKlarTilBehandlingEllerUnderBehandling()
        startMetrikkJob()
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
