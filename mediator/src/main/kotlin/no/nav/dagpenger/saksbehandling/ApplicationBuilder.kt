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
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.api.RelevanteJournalpostIdOppslag
import no.nav.dagpenger.saksbehandling.api.installerApis
import no.nav.dagpenger.saksbehandling.behandling.BehandlingHttpKlient
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.saksbehandling.db.klage.PostgresKlageRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.frist.OppgaveFristUtgåttJob
import no.nav.dagpenger.saksbehandling.job.Job.Companion.Minutt
import no.nav.dagpenger.saksbehandling.job.Job.Companion.now
import no.nav.dagpenger.saksbehandling.journalpostid.MottakHttpKlient
import no.nav.dagpenger.saksbehandling.metrikker.MetrikkJob
import no.nav.dagpenger.saksbehandling.mottak.ArenaSinkVedtakOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingAvbruttMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilVedtakMottak
import no.nav.dagpenger.saksbehandling.mottak.MeldingOmVedtakProdusentBehovløser
import no.nav.dagpenger.saksbehandling.mottak.VedtakFattetMottak
import no.nav.dagpenger.saksbehandling.pdl.PDLHttpKlient
import no.nav.dagpenger.saksbehandling.saksbehandler.CachedSaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslagImpl
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingConsumer
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingHttpKlient
import no.nav.dagpenger.saksbehandling.statistikk.PostgresStatistikkTjeneste
import no.nav.dagpenger.saksbehandling.streams.kafka.KafkaStreamsPlugin
import no.nav.dagpenger.saksbehandling.streams.kafka.kafkaStreams
import no.nav.dagpenger.saksbehandling.streams.leesah.adressebeskyttetStream
import no.nav.dagpenger.saksbehandling.streams.skjerming.skjermetPersonStatus
import no.nav.dagpenger.saksbehandling.utsending.UtsendingAlarmJob
import no.nav.dagpenger.saksbehandling.utsending.UtsendingAlarmRepository
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingBehovLøsningMottak
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingMottak
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import no.nav.helse.rapids_rivers.RapidApplication
import java.util.Timer

internal class ApplicationBuilder(configuration: Map<String, String>) : RapidsConnection.StatusListener {
    private val personRepository = PostgresPersonRepository(dataSource)
    private val oppgaveRepository = PostgresOppgaveRepository(dataSource)
    private val utsendingRepository = PostgresUtsendingRepository(dataSource)
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
        MottakHttpKlient(
            dpMottakApiUrl = Configuration.dpMottakApiUrl,
            tokenProvider = Configuration.journalpostTokenProvider,
        )
    private val behandlingKlient =
        BehandlingHttpKlient(
            dpBehandlingApiUrl = Configuration.dbBehandlingApiUrl,
            tokenProvider = Configuration.dpBehandlingOboExchanger,
        )
    private val utsendingMediator = UtsendingMediator(utsendingRepository)
    private val skjermingConsumer = SkjermingConsumer(personRepository)
    private val adressebeskyttelseConsumer = AdressebeskyttelseConsumer(personRepository, pdlKlient)
    private val saksbehandlerOppslag =
        CachedSaksbehandlerOppslag(SaksbehandlerOppslagImpl(tokenProvider = Configuration.entraTokenProvider))
    private val oppslag: Oppslag =
        Oppslag(
            pdlKlient = pdlKlient,
            relevanteJournalpostIdOppslag = RelevanteJournalpostIdOppslag(journalpostIdClient, utsendingRepository),
            saksbehandlerOppslag = saksbehandlerOppslag,
            skjermingKlient = skjermingKlient,
        )
    private val oppgaveMediator =
        OppgaveMediator(
            personRepository = personRepository,
            oppgaveRepository = oppgaveRepository,
            oppslag = oppslag,
            behandlingKlient = behandlingKlient,
            utsendingMediator = utsendingMediator,
            meldingOmVedtakKlient =
                MeldingOmVedtakKlient(
                    dpMeldingOmVedtakUrl = Configuration.dpMeldingOmVedtakBaseUrl,
                    tokenProvider = Configuration.dpMeldingOmVedtakOboExchanger,
                ),
        )
    private val oppgaveDTOMapper =
        OppgaveDTOMapper(
            oppslag = oppslag,
            oppgaveHistorikkDTOMapper = OppgaveHistorikkDTOMapper(oppgaveRepository, saksbehandlerOppslag),
        )
    private val utsendingAlarmJob: Timer
    private val slettGamleOppgaverJob: Timer
    private val oppgaveFristUtgåttJob: Timer
    private val metrikkJob: Timer

    private val rapidsConnection: RapidsConnection =
        RapidApplication.create(
            env = configuration,
            builder = {
                withKtorModule {
                    installerApis(
                        oppgaveMediator = oppgaveMediator,
                        oppgaveDTOMapper = oppgaveDTOMapper,
                        statistikkTjeneste = PostgresStatistikkTjeneste(dataSource),
                        klageMediator =
                            KlageMediator(
                                klageRepository = PostgresKlageRepository(dataSource),
                                oppgaveMediator = oppgaveMediator,
                                utsendingMediator = utsendingMediator,
                            ),
                    )
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
            VedtakFattetMottak(rapidsConnection, oppgaveMediator)
            BehandlingOpprettetMottak(rapidsConnection, oppgaveMediator, pdlKlient, skjermingKlient)
            BehandlingAvbruttMottak(rapidsConnection, oppgaveMediator)
            ForslagTilVedtakMottak(rapidsConnection, oppgaveMediator)
            UtsendingMottak(rapidsConnection, utsendingMediator)
            UtsendingBehovLøsningMottak(rapidsConnection, utsendingMediator)
            ArenaSinkVedtakOpprettetMottak(
                rapidsConnection,
                oppgaveRepository,
                utsendingMediator,
            )
            MeldingOmVedtakProdusentBehovløser(rapidsConnection, utsendingMediator)
            utsendingAlarmJob =
                UtsendingAlarmJob(rapidsConnection, UtsendingAlarmRepository(dataSource)).startJob(
                    period = 60.Minutt,
                )
            slettGamleOppgaverJob =
                SletteGamleOppgaverJob(
                    rapidsConnection,
                    GamleOppgaverRepository(dataSource),
                ).startJob()
            oppgaveFristUtgåttJob =
                OppgaveFristUtgåttJob(oppgaveMediator).startJob()
            metrikkJob =
                MetrikkJob().startJob(
                    startAt = now,
                    period = 5.Minutt,
                )
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
        utsendingAlarmJob.cancel()
        slettGamleOppgaverJob.cancel()
        oppgaveFristUtgåttJob.cancel()
        metrikkJob.cancel()
        logger.info { "Skrur av applikasjonen" }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
