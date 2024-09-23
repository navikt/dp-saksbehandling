package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.getSnapShot
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AdressebeskyttelseConsumerTest {
    @Test
    fun `test oppdaterAdressebeskyttelseStatus`() {
        val repository =
            mockk<AdressebeskyttelseRepository>(relaxed = true).also {
                every { it.eksistererIDPsystem(setOf("1", "2")) } returns setOf("1", "2")
            }
        val pdlKlient =
            mockk<PDLKlient>().also { pdlKlient ->
                io.mockk.coEvery { pdlKlient.person("1") } returns testPersonResultat("1", STRENGT_FORTROLIG)
                io.mockk.coEvery { pdlKlient.person("2") } returns testPersonResultat("2", UGRADERT)
            }

        val registry = PrometheusRegistry()
        val adressebeskyttelseConsumer =
            AdressebeskyttelseConsumer(
                repository = repository,
                pdlKlient = pdlKlient,
                registry = registry,
            )
        adressebeskyttelseConsumer.oppdaterAdressebeskyttelseStatus(setOf("2", "1"))

        verify(exactly = 1) {
            repository.oppdaterAdressebeskyttetStatus("1", STRENGT_FORTROLIG)
        }
        verify(exactly = 1) {
            repository.oppdaterAdressebeskyttetStatus("2", UGRADERT)
        }

        registry.getSnapShot<CounterSnapshot> { it == "dp_saksbehandling_adressebeskyttelse_oppdateringer" }.let { snapshot ->
            snapshot.dataPoints.single { it.labels["status"] == STRENGT_FORTROLIG.name }.value shouldBe 1.0
            snapshot.dataPoints.single { it.labels["status"] == UGRADERT.name }.value shouldBe 1.0
        }
    }

    private fun testPersonResultat(
        fnr: String,
        adresseBeskyttelseGradering: AdressebeskyttelseGradering,
    ): Result<PDLPersonIntern> {
        return Result.success(
            PDLPersonIntern(
                ident = fnr,
                fornavn = "Ola",
                etternavn = "Nordmann",
                mellomnavn = null,
                fødselsdato = LocalDate.of(1990, 1, 1),
                alder = 31,
                statsborgerskap = "NOR",
                kjønn = PDLPerson.Kjonn.MANN,
                adresseBeskyttelseGradering = adresseBeskyttelseGradering,
            ),
        )
    }
}
