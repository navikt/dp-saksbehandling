package no.nav.dagpenger.behandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.api.arbeidsforholdApi
import no.nav.dagpenger.behandling.api.oppgaveApi
import no.nav.dagpenger.behandling.arbeidsforhold.AaregClient
import no.nav.dagpenger.behandling.db.HardkodedVurderingRepository
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.behandling.db.PostgresRepository
import no.nav.dagpenger.behandling.hendelser.mottak.SøknadMottak
import no.nav.dagpenger.behandling.iverksett.IverksettClient
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

internal class ApplicationBuilder(configuration: Map<String, String>) : RapidsConnection.StatusListener {
    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(configuration))
            .withKtorModule {
                oppgaveApi(mediator = mediator)
                arbeidsforholdApi(aaregClient = AaregClient())
            }.build()

    private val postgresRepository = PostgresRepository(dataSource)
    private val mediator =
        Mediator(
            rapidsConnection = rapidsConnection,
            oppgaveRepository = postgresRepository,
            personRepository = postgresRepository,
            behandlingRepository = postgresRepository,
            aktivitetsloggMediator = AktivitetsloggMediator(rapidsConnection),
            iverksettClient = IverksettClient(),
            vurderingRepository = HardkodedVurderingRepository(),
        )

    init {
        rapidsConnection.register(this)
        SøknadMottak(
            rapidsConnection = rapidsConnection,
            mediator,
        )
    }

    fun start() {
        rapidsConnection.start()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        // clean()
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
