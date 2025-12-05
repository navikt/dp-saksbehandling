package no.nav.dagpenger.saksbehandling

import PersonMediator
import com.github.navikt.tbd_libs.rapids_and_rivers.KafkaRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.install
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import no.nav.dagpenger.saksbehandling.adressebeskyttelse.AdressebeskyttelseConsumer
import no.nav.dagpenger.saksbehandling.api.KlageDTOMapper
import no.nav.dagpenger.saksbehandling.api.OppgaveDTOMapper
import no.nav.dagpenger.saksbehandling.api.OppgaveHistorikkDTOMapper
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.api.RelevanteJournalpostIdOppslag
import no.nav.dagpenger.saksbehandling.api.installerApis
import no.nav.dagpenger.saksbehandling.audit.ApiAuditlogg
import no.nav.dagpenger.saksbehandling.behandling.BehandlingHttpKlient
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.saksbehandling.db.innsending.PostgresInnsendingRepository
import no.nav.dagpenger.saksbehandling.db.klage.PostgresKlageRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.frist.OppgaveFristUtgåttJob
import no.nav.dagpenger.saksbehandling.innsending.InnsendingAlarmJob
import no.nav.dagpenger.saksbehandling.innsending.InnsendingAlarmRepository
import no.nav.dagpenger.saksbehandling.innsending.InnsendingBehandler
import no.nav.dagpenger.saksbehandling.innsending.InnsendingMediator
import no.nav.dagpenger.saksbehandling.job.Job.Companion.Dag
import no.nav.dagpenger.saksbehandling.job.Job.Companion.Minutt
import no.nav.dagpenger.saksbehandling.job.Job.Companion.getNextOccurrence
import no.nav.dagpenger.saksbehandling.job.Job.Companion.now
import no.nav.dagpenger.saksbehandling.journalpostid.MottakHttpKlient
import no.nav.dagpenger.saksbehandling.klage.OversendKlageinstansAlarmJob
import no.nav.dagpenger.saksbehandling.klage.OversendKlageinstansAlarmRepository
import no.nav.dagpenger.saksbehandling.klage.OversendtKlageinstansMottak
import no.nav.dagpenger.saksbehandling.metrikker.MetrikkJob
import no.nav.dagpenger.saksbehandling.mottak.ArenaSinkVedtakOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingAvbruttMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingsresultatMottak
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilBehandlingsresultatMottak
import no.nav.dagpenger.saksbehandling.mottak.InnsendingBehovløser
import no.nav.dagpenger.saksbehandling.mottak.MeldingOmVedtakProdusentBehovløser
import no.nav.dagpenger.saksbehandling.mottak.SøknadBehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.oppgave.OppgaveTilstandAlertJob
import no.nav.dagpenger.saksbehandling.pdl.PDLHttpKlient
import no.nav.dagpenger.saksbehandling.sak.BehandlingsresultatMottakForSak
import no.nav.dagpenger.saksbehandling.sak.SakMediator
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
import no.nav.dagpenger.saksbehandling.utsending.mottak.BehandlingsresultatMottakForUtsending
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingBehovLøsningMottak
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import no.nav.helse.rapids_rivers.RapidApplication
import java.util.Timer

internal class ApplicationBuilder(
    configuration: Map<String, String>,
) : RapidsConnection.StatusListener {
    private val klageRepository = PostgresKlageRepository(dataSource)
    private val oppgaveRepository = PostgresOppgaveRepository(dataSource)
    private val personRepository = PostgresPersonRepository(dataSource)
    private val sakRepository = PostgresSakRepository(dataSource = dataSource)
    private val utsendingRepository = PostgresUtsendingRepository(dataSource)
    private val innsendingRepository = PostgresInnsendingRepository(dataSource)

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

    private val skjermingConsumer = SkjermingConsumer(personRepository)
    private val adressebeskyttelseConsumer = AdressebeskyttelseConsumer(personRepository, pdlKlient)
    private val saksbehandlerOppslag =
        CachedSaksbehandlerOppslag(SaksbehandlerOppslagImpl(tokenProvider = Configuration.entraTokenProvider))
    private val oppslag: Oppslag =
        Oppslag(
            pdlKlient = pdlKlient,
            relevanteJournalpostIdOppslag =
                RelevanteJournalpostIdOppslag(
                    journalpostIdKlient = journalpostIdClient,
                    utsendingRepository = utsendingRepository,
                    klageRepository = klageRepository,
                    innsendingRepository = innsendingRepository,
                ),
            saksbehandlerOppslag = saksbehandlerOppslag,
            skjermingKlient = skjermingKlient,
        )
    private val personMediator =
        PersonMediator(
            personRepository = personRepository,
            oppslag = oppslag,
        )
    private val sakMediator =
        SakMediator(
            personMediator = personMediator,
            sakRepository = sakRepository,
        )

    private val meldingOmVedtakKlient =
        MeldingOmVedtakKlient(
            dpMeldingOmVedtakUrl = Configuration.dpMeldingOmVedtakBaseUrl,
            tokenProvider = Configuration.dpMeldingOmVedtakOboExchanger,
        )

    private val brevProdusent =
        UtsendingMediator.BrevProdusent(
            oppslag = oppslag,
            meldingOmVedtakKlient = meldingOmVedtakKlient,
            oppgaveRepository = oppgaveRepository,
            tokenProvider = Configuration.meldingOmVedtakMaskinTokenProvider,
        )
    private val utsendingMediator =
        UtsendingMediator(
            utsendingRepository = utsendingRepository,
            brevProdusent = brevProdusent,
        )
    private val oppgaveMediator =
        OppgaveMediator(
            oppgaveRepository = oppgaveRepository,
            behandlingKlient = behandlingKlient,
            utsendingMediator = utsendingMediator,
            sakMediator = sakMediator,
        )
    private val klageMediator =
        KlageMediator(
            klageRepository = klageRepository,
            oppgaveMediator = oppgaveMediator,
            utsendingMediator = utsendingMediator,
            oppslag = oppslag,
            meldingOmVedtakKlient = meldingOmVedtakKlient,
            sakMediator = sakMediator,
        )

    private val innsendingMediator =
        InnsendingMediator(
            sakMediator = sakMediator,
            oppgaveMediator = oppgaveMediator,
            personMediator = personMediator,
            innsendingRepository = innsendingRepository,
            innsendingBehandler =
                InnsendingBehandler(
                    klageMediator = klageMediator,
                    behandlingKlient = behandlingKlient,
                ),
        )
    private val oppgaveDTOMapper =
        OppgaveDTOMapper(
            oppslag = oppslag,
            oppgaveHistorikkDTOMapper = OppgaveHistorikkDTOMapper(oppgaveRepository, saksbehandlerOppslag),
            sakMediator = sakMediator,
        )
    private val innsendingAlarmJob: Timer
    private val utsendingAlarmJob: Timer
    private val oversendKlageinstansAlarmJob: Timer
    private val oppgaveFristUtgåttJob: Timer
    private val metrikkJob: Timer
    private val oppgaveTilstandAlertJob: Timer

    private val rapidsConnection: RapidsConnection =
        RapidApplication
            .create(
                env = configuration,
                builder = {
                    withKtorModule {
                        installerApis(
                            oppgaveMediator = oppgaveMediator,
                            oppgaveDTOMapper = oppgaveDTOMapper,
                            statistikkTjeneste = PostgresStatistikkTjeneste(dataSource),
                            klageMediator = klageMediator,
                            klageDTOMapper = KlageDTOMapper(oppslag),
                            personMediator = personMediator,
                            sakMediator = sakMediator,
                            innsendingMediator = innsendingMediator,
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
                                        adressebeskyttelseConsumer::oppdaterAdressebeskyttelseGradering,
                                    )
                                }
                        }
                    }
                },
            ) { _: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>, _: KafkaRapid ->
            }.also { rapidsConnection ->
                sakMediator.setRapidsConnection(rapidsConnection)
                utsendingMediator.setRapidsConnection(rapidsConnection)
                oppgaveMediator.setRapidsConnection(rapidsConnection)
                klageMediator.setRapidsConnection(rapidsConnection)
                klageMediator.setAuditlogg(ApiAuditlogg(AktivitetsloggMediator(), rapidsConnection))
                BehandlingOpprettetMottak(rapidsConnection, sakMediator)
                SøknadBehandlingOpprettetMottak(rapidsConnection, innsendingMediator)
                BehandlingAvbruttMottak(rapidsConnection, oppgaveMediator)
                BehandlingsresultatMottak(rapidsConnection, oppgaveMediator)
                ForslagTilBehandlingsresultatMottak(rapidsConnection, oppgaveMediator)
                UtsendingBehovLøsningMottak(rapidsConnection, utsendingMediator)
                InnsendingBehovløser(
                    rapidsConnection = rapidsConnection,
                    innsendingMediator = innsendingMediator,
                )

                BehandlingsresultatMottakForUtsending(
                    rapidsConnection = rapidsConnection,
                    utsendingMediator = utsendingMediator,
                    sakRepository = sakRepository,
                )
                BehandlingsresultatMottakForSak(
                    rapidsConnection = rapidsConnection,
                    sakRepository = sakRepository,
                    sakMediator = sakMediator,
                )

                ArenaSinkVedtakOpprettetMottak(
                    rapidsConnection = rapidsConnection,
                    personRepository = personRepository,
                    utsendingMediator = utsendingMediator,
                    sakMediator = sakMediator,
                )
                MeldingOmVedtakProdusentBehovløser(rapidsConnection, utsendingMediator)
                OversendtKlageinstansMottak(
                    rapidsConnection = rapidsConnection,
                    klageMediator = klageMediator,
                )
                utsendingAlarmJob =
                    UtsendingAlarmJob(
                        rapidsConnection = rapidsConnection,
                        utsendingAlarmRepository = UtsendingAlarmRepository(dataSource),
                    ).startJob(
                        period = 60.Minutt,
                    )
                innsendingAlarmJob =
                    InnsendingAlarmJob(
                        rapidsConnection = rapidsConnection,
                        innsendingAlarmRepository = InnsendingAlarmRepository(dataSource),
                    ).startJob(
                        period = 1.Dag,
                    )
                oppgaveTilstandAlertJob =
                    OppgaveTilstandAlertJob(
                        rapidsConnection = rapidsConnection,
                        oppgaveMediator = oppgaveMediator,
                    ).startJob(
                        period = 1.Dag,
                    )

                oversendKlageinstansAlarmJob =
                    OversendKlageinstansAlarmJob(
                        rapidsConnection = rapidsConnection,
                        repository = OversendKlageinstansAlarmRepository(dataSource),
                    ).startJob(
                        period = 60.Minutt,
                    )
                oppgaveFristUtgåttJob =
                    OppgaveFristUtgåttJob(oppgaveMediator).startJob(
                        startAt = getNextOccurrence(3, 0),
                        period = 1.Dag,
                    )
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
        oversendKlageinstansAlarmJob.cancel()
        oppgaveFristUtgåttJob.cancel()
        metrikkJob.cancel()
        oppgaveTilstandAlertJob.cancel()
        innsendingAlarmJob.cancel()
        logger.info { "Skrur av applikasjonen" }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
