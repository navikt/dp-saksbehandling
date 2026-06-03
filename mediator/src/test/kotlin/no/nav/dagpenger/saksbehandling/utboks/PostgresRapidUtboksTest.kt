package no.nav.dagpenger.saksbehandling.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.kotest.matchers.shouldBe
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Postgres
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import no.nav.dagpenger.saksbehandling.getSnapShot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class PostgresRapidUtboksTest {
    private val testRapid = TestRapid()

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `send skriver til utboks-tabell i samme transaksjon`() {
        Postgres.withMigratedDb { ds ->
            val databaseSession = DatabaseSession(ds)
            val transaksjoner = Transaksjoner(databaseSession)
            val repository = PostgresUtboksRepository(databaseSession)
            val registry = PrometheusRegistry()

            val utboks =
                PostgresRapidUtboks(
                    repository = repository,
                    rapidsConnection = testRapid,
                    registry = registry,
                )

            transaksjoner.transaksjon { ctx ->
                utboks.send(key = "12345678901", message = """{"@event_name":"test"}""", ctx = ctx)
            }

            repository.hentMedTilstand(UtboksTilstand.PENDING.name).size shouldBe 1
            registry.getSnapShot<CounterSnapshot> { it == "dp_saksbehandling_utboks_nye_meldinger_total" }.let { snapshot ->
                snapshot.dataPoints.single().value shouldBe 1.0
            }
        }
    }

    @Test
    fun `send ruller tilbake ved feil i samme transaksjon`() {
        Postgres.withMigratedDb { ds ->
            val databaseSession = DatabaseSession(ds)
            val transaksjoner = Transaksjoner(databaseSession)
            val repository = PostgresUtboksRepository(databaseSession)
            val registry = PrometheusRegistry()

            val utboks =
                PostgresRapidUtboks(
                    repository = repository,
                    rapidsConnection = testRapid,
                    registry = registry,
                )

            runCatching {
                transaksjoner.transaksjon { ctx ->
                    utboks.send(key = "12345678901", message = """{"@event_name":"test"}""", ctx = ctx)
                    utboks.send(key = "12345678902", message = """{"@event_name":"test2"}""", ctx = ctx)
                    error("Simulert feil etter send")
                }
            }
            repository.hentMedTilstand(UtboksTilstand.PENDING.name).size shouldBe 0
        }
    }

    @Test
    fun `publiserVentende publiserer PENDING meldinger i FIFO-rekkefølge og markerer som SENDT`() {
        Postgres.withMigratedDb { ds ->
            val databaseSession = DatabaseSession(ds)
            val transaksjoner = Transaksjoner(databaseSession)
            val repository = PostgresUtboksRepository(databaseSession)
            val registry = PrometheusRegistry()

            val utboks =
                PostgresRapidUtboks(
                    repository = repository,
                    rapidsConnection = testRapid,
                    registry = registry,
                )
            transaksjoner.transaksjon { ctx ->
                utboks.send(key = "a", message = """{"ident":"a"}""", ctx = ctx)
                utboks.send(key = "b", message = """{"ident":"b"}""", ctx = ctx)
                utboks.send(key = "c", message = """{"ident":"c"}""", ctx = ctx)
            }

            utboks.publiserVentende()

            testRapid.inspektør.size shouldBe 3

            testRapid.inspektør.key(0) shouldBe "a"
            testRapid.inspektør.message(0)["ident"].asString() shouldBe "a"

            testRapid.inspektør.key(1) shouldBe "b"
            testRapid.inspektør.message(1)["ident"].asString() shouldBe "b"

            testRapid.inspektør.key(2) shouldBe "c"
            testRapid.inspektør.message(2)["ident"].asString() shouldBe "c"

            repository.hentMedTilstand(UtboksTilstand.PENDING.name).size shouldBe 0
            repository.hentMedTilstand(UtboksTilstand.SENDT.name).size shouldBe 3
        }
    }

    @Test
    fun `publiserVentende stopper ved første feil og lar resten stå i PENDING`() {
        Postgres.withMigratedDb { ds ->
            val databaseSession = DatabaseSession(ds)
            val transaksjoner = Transaksjoner(databaseSession)
            val repository = PostgresUtboksRepository(databaseSession)
            val registry = PrometheusRegistry()

            val utboks =
                PostgresRapidUtboks(
                    repository = repository,
                    rapidsConnection = FeilendeRapid(1, testRapid),
                    registry = registry,
                )

            transaksjoner.transaksjon { ctx ->
                utboks.send(key = "x", message = """{"ident":"x"}""", ctx = ctx)
                utboks.send(key = "y", message = """{"ident":"y"}""", ctx = ctx)
            }

            utboks.publiserVentende()

            repository.hentMedTilstand(UtboksTilstand.SENDT.name).single().let {
                it.key shouldBe "x"
                it.message shouldBe """{"ident":"x"}"""
            }
            repository.hentMedTilstand(UtboksTilstand.PENDING.name).single().let {
                it.key shouldBe "y"
                it.message shouldBe """{"ident":"y"}"""
            }

            registry.getSnapShot<CounterSnapshot> { it == "dp_saksbehandling_utboks_publiserte_meldinger_total" }.let { snapshot ->
                snapshot.dataPoints.single { it.labels["status"] == "success" }.value shouldBe 1.0
                snapshot.dataPoints.single { it.labels["status"] == "failed" }.value shouldBe 1.0
            }
        }
    }

    @Test
    fun `publiserVentende publiserer ikke når ingen PENDING meldinger finnes`() {
        Postgres.withMigratedDb { ds ->
            val databaseSession = DatabaseSession(ds)
            val repository = PostgresUtboksRepository(databaseSession)
            val registry = PrometheusRegistry()

            PostgresRapidUtboks(
                repository = repository,
                rapidsConnection = testRapid,
                registry = registry,
            ).publiserVentende()

            testRapid.inspektør.size shouldBe 0
        }
    }

    @Test
    fun `slettGamleSendte rydder bort sendte meldinger eldre enn levetiden`() {
        Postgres.withMigratedDb { ds ->
            val databaseSession = DatabaseSession(ds)
            val transaksjoner = Transaksjoner(databaseSession)
            val repository = PostgresUtboksRepository(databaseSession)
            val registry = PrometheusRegistry()

            val utboks =
                PostgresRapidUtboks(
                    repository = repository,
                    // Negativ levetid gjør at cutoff havner i framtiden, slik at nylig sendte
                    // meldinger regnes som utgått — uten å måtte manipulere created_at direkte.
                    rapidsConnection = testRapid,
                    levetidSendte = Duration.ofDays(-2),
                    registry = registry,
                )

            transaksjoner.transaksjon { ctx ->
                utboks.send(key = "gammel", message = """{"ident":"g"}""", ctx = ctx)
            }

            utboks.publiserVentende()
            repository.hentMedTilstand(UtboksTilstand.SENDT.name).size shouldBe 1

            utboks.slettGamleSendte() shouldBe 1
            repository.hentMedTilstand(UtboksTilstand.SENDT.name).size shouldBe 0
        }
    }
}

private class FeilendeRapid(
    private val feilPåMeldingNummer: Int,
    private val delegate: RapidsConnection,
) : RapidsConnection() {
    private var antallPublisert = 0

    private fun failOrInc() {
        if (antallPublisert == feilPåMeldingNummer) {
            throw RuntimeException("Simulert Kafka-feil")
        } else {
            antallPublisert++
        }
    }

    override fun publish(message: String) {
        failOrInc()
        delegate.publish(message)
    }

    override fun publish(
        key: String,
        message: String,
    ) {
        failOrInc()
        delegate.publish(key, message)
    }

    override fun publish(messages: List<OutgoingMessage>) = TODO()

    override fun rapidName(): String = "FailOnFirstPublishRapid"

    override fun start() {}

    override fun stop() {}
}
