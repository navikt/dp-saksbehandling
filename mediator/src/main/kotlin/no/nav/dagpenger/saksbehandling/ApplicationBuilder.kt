package no.nav.dagpenger.saksbehandling

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
import no.nav.dagpenger.saksbehandling.db.oppfolging.PostgresOppfølgingRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
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
import no.nav.dagpenger.saksbehandling.klage.KlageBehandlingUtførtMottakForOppgave
import no.nav.dagpenger.saksbehandling.klage.KlageBehandlingUtførtMottakForUtsending
import no.nav.dagpenger.saksbehandling.klage.KlageinstansVedtakMottak
import no.nav.dagpenger.saksbehandling.klage.OversendKlageinstansAlarmJob
import no.nav.dagpenger.saksbehandling.klage.OversendKlageinstansAlarmRepository
import no.nav.dagpenger.saksbehandling.klage.OversendtKlageinstansMottak
import no.nav.dagpenger.saksbehandling.klage.UtsendingDistribuertMottakForKlage
import no.nav.dagpenger.saksbehandling.metrikker.MetrikkJob
import no.nav.dagpenger.saksbehandling.mottak.ArenaSinkVedtakOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingAvbruttMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingsresultatMottak
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilBehandlingsresultatMottak
import no.nav.dagpenger.saksbehandling.mottak.InnsendingBehovløser
import no.nav.dagpenger.saksbehandling.mottak.MeldingOmVedtakProdusentBehovløser
import no.nav.dagpenger.saksbehandling.mottak.SøknadBehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.SøknadsavklaringLøsningMottak
import no.nav.dagpenger.saksbehandling.oppfolging.OppfølgingAlarmJob
import no.nav.dagpenger.saksbehandling.oppfolging.OppfølgingAlarmRepository
import no.nav.dagpenger.saksbehandling.oppfolging.OppfølgingBehandler
import no.nav.dagpenger.saksbehandling.oppfolging.OppfølgingMediator
import no.nav.dagpenger.saksbehandling.oppfolging.OpprettOppgaveMottak
import no.nav.dagpenger.saksbehandling.oppgave.OppgaveTilstandAlertJob
import no.nav.dagpenger.saksbehandling.pdl.PDLHttpKlient
import no.nav.dagpenger.saksbehandling.sak.BehandlingsresultatMottakForSak
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.saksbehandler.CachedSaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslagImpl
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingConsumer
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingHttpKlient
import no.nav.dagpenger.saksbehandling.statistikk.StatistikkJob
import no.nav.dagpenger.saksbehandling.statistikk.db.PostgresProduksjonsstatistikkRepository
import no.nav.dagpenger.saksbehandling.statistikk.db.PostgresSaksbehandlingsstatistikkRepository
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
    private val personRepository = PostgresPersonRepository(dataSource)
    private val pdlKlient =
        PDLHttpKlient(
            url = Configuration.pdlUrl,
            tokenSupplier = Configuration.pdlTokenProvider,
        )

    private lateinit var innsendingAlarmJob: Timer
    private lateinit var utsendingAlarmJob: Timer
    private lateinit var oversendKlageinstansAlarmJob: Timer
    private lateinit var oppfølgingAlarmJob: Timer
    private lateinit var oppgaveFristUtgåttJob: Timer
    private lateinit var metrikkJob: Timer
    private lateinit var statistikkJob: Timer
    private lateinit var oppgaveTilstandAlertJob: Timer

    private val rapidsConnection: RapidsConnection =
        RapidApplication
            .create(
                env = configuration,
            ) { server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>, rapid: KafkaRapid ->
                val oppgaveRepository = PostgresOppgaveRepository(dataSource)
                val sakRepository = PostgresSakRepository(dataSource = dataSource)
                val utsendingRepository = PostgresUtsendingRepository(dataSource)
                val innsendingRepository = PostgresInnsendingRepository(dataSource)
                val klageRepository = PostgresKlageRepository(dataSource)

                val behandlingKlient =
                    BehandlingHttpKlient(
                        dpBehandlingApiUrl = Configuration.dbBehandlingApiUrl,
                        tokenProvider = Configuration.dpBehandlingOboExchanger,
                    )
                val meldingOmVedtakKlient =
                    MeldingOmVedtakKlient(
                        dpMeldingOmVedtakUrl = Configuration.dpMeldingOmVedtakBaseUrl,
                        tokenProvider = Configuration.dpMeldingOmVedtakOboExchanger,
                    )
                val saksbehandlerOppslag =
                    CachedSaksbehandlerOppslag(SaksbehandlerOppslagImpl(tokenProvider = Configuration.entraTokenProvider))
                val oppslag =
                    Oppslag(
                        pdlKlient = pdlKlient,
                        relevanteJournalpostIdOppslag =
                            RelevanteJournalpostIdOppslag(
                                journalpostIdKlient =
                                    MottakHttpKlient(
                                        dpMottakApiUrl = Configuration.dpMottakApiUrl,
                                        tokenProvider = Configuration.journalpostTokenProvider,
                                    ),
                                utsendingRepository = utsendingRepository,
                                klageRepository = klageRepository,
                                innsendingRepository = innsendingRepository,
                            ),
                        saksbehandlerOppslag = saksbehandlerOppslag,
                        skjermingKlient =
                            SkjermingHttpKlient(
                                skjermingApiUrl = Configuration.skjermingApiUrl,
                                tokenProvider = Configuration.skjermingTokenProvider,
                            ),
                    )
                val personMediator =
                    PersonMediator(
                        personRepository = personRepository,
                        oppslag = oppslag,
                    )

                val sakMediator =
                    SakMediator(
                        personMediator = personMediator,
                        sakRepository = sakRepository,
                        rapidsConnection = rapid,
                    )
                val utsendingMediator =
                    UtsendingMediator(
                        utsendingRepository = utsendingRepository,
                        brevProdusent =
                            UtsendingMediator.BrevProdusent(
                                oppslag = oppslag,
                                meldingOmVedtakKlient = meldingOmVedtakKlient,
                                oppgaveRepository = oppgaveRepository,
                                tokenProvider = Configuration.meldingOmVedtakMaskinTokenProvider,
                            ),
                        rapidsConnection = rapid,
                    )
                val oppgaveMediator =
                    OppgaveMediator(
                        oppgaveRepository = oppgaveRepository,
                        behandlingKlient = behandlingKlient,
                        utsendingMediator = utsendingMediator,
                        sakMediator = sakMediator,
                        rapidsConnection = rapid,
                    )
                val klageMediator =
                    KlageMediator(
                        klageRepository = klageRepository,
                        oppgaveMediator = oppgaveMediator,
                        utsendingMediator = utsendingMediator,
                        oppslag = oppslag,
                        meldingOmVedtakKlient = meldingOmVedtakKlient,
                        sakMediator = sakMediator,
                        rapidsConnection = rapid,
                    )
                val oppfølgingMediator =
                    OppfølgingMediator(
                        oppfølgingRepository = PostgresOppfølgingRepository(dataSource),
                        oppfølgingBehandler =
                            OppfølgingBehandler(
                                klageMediator = klageMediator,
                                behandlingKlient = behandlingKlient,
                            ),
                        personMediator = personMediator,
                        sakMediator = sakMediator,
                        oppgaveMediator = oppgaveMediator,
                    )
                val innsendingMediator =
                    InnsendingMediator(
                        sakMediator = sakMediator,
                        oppgaveMediator = oppgaveMediator,
                        personMediator = personMediator,
                        innsendingRepository = innsendingRepository,
                        innsendingBehandler =
                            InnsendingBehandler(
                                klageMediator = klageMediator,
                                behandlingKlient = behandlingKlient,
                                oppfølgingMediator = oppfølgingMediator,
                            ),
                    )

                server.application.apply {
                    installerApis(
                        oppgaveMediator = oppgaveMediator,
                        oppgaveDTOMapper =
                            OppgaveDTOMapper(
                                oppslag = oppslag,
                                oppgaveHistorikkDTOMapper =
                                    OppgaveHistorikkDTOMapper(
                                        oppgaveRepository,
                                        saksbehandlerOppslag,
                                    ),
                                sakMediator = sakMediator,
                            ),
                        produksjonsstatistikkRepository = PostgresProduksjonsstatistikkRepository(dataSource),
                        klageMediator = klageMediator,
                        klageDTOMapper = KlageDTOMapper(oppslag),
                        personMediator = personMediator,
                        sakMediator = sakMediator,
                        innsendingMediator = innsendingMediator,
                        meldingOmVedtakMediator =
                            MeldingOmVedtakMediator(
                                oppgaveMediator = oppgaveMediator,
                                meldingOmVedtakKlient = meldingOmVedtakKlient,
                                oppslag = oppslag,
                                sakMediator = sakMediator,
                            ),
                        oppfølgingMediator = oppfølgingMediator,
                        auditlogg = ApiAuditlogg(AktivitetsloggMediator(), rapid),
                    )

                    install(KafkaStreamsPlugin) {
                        kafkaStreams =
                            kafkaStreams(Configuration.kafkaStreamProperties) {
                                skjermetPersonStatus(
                                    Configuration.skjermingPersonStatusTopic,
                                    SkjermingConsumer(personRepository)::oppdaterSkjermetStatus,
                                )
                                adressebeskyttetStream(
                                    Configuration.leesahTopic,
                                    AdressebeskyttelseConsumer(
                                        personRepository,
                                        pdlKlient,
                                    )::oppdaterAdressebeskyttelseGradering,
                                )
                            }
                    }
                }

                registrerMottak(
                    rapid = rapid,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                    utsendingMediator = utsendingMediator,
                    klageMediator = klageMediator,
                    innsendingMediator = innsendingMediator,
                    oppfølgingMediator = oppfølgingMediator,
                    sakRepository = sakRepository,
                )

                startJobber(
                    rapid = rapid,
                    oppgaveMediator = oppgaveMediator,
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
        oppfølgingAlarmJob.cancel()
        oppgaveFristUtgåttJob.cancel()
        metrikkJob.cancel()
        statistikkJob.cancel()
        oppgaveTilstandAlertJob.cancel()
        innsendingAlarmJob.cancel()
        logger.info { "Skrur av applikasjonen" }
    }

    private fun registrerMottak(
        rapid: KafkaRapid,
        sakMediator: SakMediator,
        oppgaveMediator: OppgaveMediator,
        utsendingMediator: UtsendingMediator,
        klageMediator: KlageMediator,
        innsendingMediator: InnsendingMediator,
        oppfølgingMediator: OppfølgingMediator,
        sakRepository: PostgresSakRepository,
    ) {
        BehandlingOpprettetMottak(rapid, sakMediator)
        SøknadBehandlingOpprettetMottak(rapid, innsendingMediator)
        BehandlingAvbruttMottak(rapid, oppgaveMediator)
        BehandlingsresultatMottak(rapid, oppgaveMediator)
        ForslagTilBehandlingsresultatMottak(rapid, oppgaveMediator)
        SøknadsavklaringLøsningMottak(rapid, oppgaveMediator)
        UtsendingBehovLøsningMottak(rapid, utsendingMediator)
        InnsendingBehovløser(
            rapidsConnection = rapid,
            innsendingMediator = innsendingMediator,
        )
        BehandlingsresultatMottakForUtsending(
            rapidsConnection = rapid,
            utsendingMediator = utsendingMediator,
            sakRepository = sakRepository,
        )
        // TODO: Kommenter inn når vi skal skru av fatting av vedtak mot Arena.
        // BehandlingsresultatMottakForAutomatiskVedtakUtsending(
        //     rapidsConnection = rapid,
        //     utsendingMediator = utsendingMediator,
        //     sakRepository = sakRepository,
        // )
        BehandlingsresultatMottakForSak(
            rapidsConnection = rapid,
            sakRepository = sakRepository,
            sakMediator = sakMediator,
        )
        ArenaSinkVedtakOpprettetMottak(
            rapidsConnection = rapid,
            personRepository = personRepository,
            utsendingMediator = utsendingMediator,
            sakMediator = sakMediator,
        )
        MeldingOmVedtakProdusentBehovløser(rapid, utsendingMediator)
        OversendtKlageinstansMottak(
            rapidsConnection = rapid,
            klageMediator = klageMediator,
        )
        KlageinstansVedtakMottak(
            rapidsConnection = rapid,
            klageMediator = klageMediator,
        )
        UtsendingDistribuertMottakForKlage(
            rapidsConnection = rapid,
            klageMediator = klageMediator,
        )
        KlageBehandlingUtførtMottakForUtsending(
            rapidsConnection = rapid,
            utsendingMediator = utsendingMediator,
        )
        KlageBehandlingUtførtMottakForOppgave(
            rapidsConnection = rapid,
            oppgaveMediator = oppgaveMediator,
        )
        OpprettOppgaveMottak(
            rapidsConnection = rapid,
            oppfølgingMediator = oppfølgingMediator,
        )
    }

    private fun startJobber(
        rapid: KafkaRapid,
        oppgaveMediator: OppgaveMediator,
    ) {
        utsendingAlarmJob =
            UtsendingAlarmJob(
                rapidsConnection = rapid,
                utsendingAlarmRepository = UtsendingAlarmRepository(dataSource),
            ).startJob(period = 60.Minutt)
        innsendingAlarmJob =
            InnsendingAlarmJob(
                rapidsConnection = rapid,
                innsendingAlarmRepository = InnsendingAlarmRepository(dataSource),
            ).startJob(period = 1.Dag)
        oppfølgingAlarmJob =
            OppfølgingAlarmJob(
                rapidsConnection = rapid,
                oppfølgingAlarmRepository = OppfølgingAlarmRepository(dataSource),
            ).startJob(period = 1.Dag)
        oppgaveTilstandAlertJob =
            OppgaveTilstandAlertJob(
                rapidsConnection = rapid,
                oppgaveMediator = oppgaveMediator,
            ).startJob(period = 1.Dag)
        oversendKlageinstansAlarmJob =
            OversendKlageinstansAlarmJob(
                rapidsConnection = rapid,
                repository = OversendKlageinstansAlarmRepository(dataSource),
            ).startJob(period = 60.Minutt)
        oppgaveFristUtgåttJob =
            OppgaveFristUtgåttJob(oppgaveMediator).startJob(
                startAt = getNextOccurrence(3, 0),
                period = 1.Dag,
            )
        statistikkJob =
            StatistikkJob(
                rapidsConnection = rapid,
                saksbehandlingsstatistikkRepository = PostgresSaksbehandlingsstatistikkRepository(dataSource),
            ).startJob(startAt = now, period = 5.Minutt)
        metrikkJob =
            MetrikkJob().startJob(startAt = now, period = 5.Minutt)
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
