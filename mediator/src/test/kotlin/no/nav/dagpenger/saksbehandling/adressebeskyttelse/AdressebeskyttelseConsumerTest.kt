package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AdressebeskyttelseConsumerTest {
    @Test
    fun `test oppdaterAdressebeskyttelseStatus`() {
        val registry =
            mockk<AdressebeskyttelseRepository>(relaxed = true).also {
                every { it.eksistererIDPsystem(setOf("1", "2")) } returns setOf("1", "2")
            }
        val pdlKlient =
            mockk<PDLKlient>().also { pdlKlient ->
                io.mockk.coEvery { pdlKlient.person("1") } returns testPersonResultat("1", STRENGT_FORTROLIG)
                io.mockk.coEvery { pdlKlient.person("2") } returns testPersonResultat("2", UGRADERT)
            }

        val adressebeskyttelseConsumer =
            AdressebeskyttelseConsumer(
                repository = registry,
                pdlKlient = pdlKlient,
                registry = PrometheusRegistry(),
            )
        adressebeskyttelseConsumer.oppdaterAdressebeskyttelseStatus(setOf("2", "1"))

        verify(exactly = 1) {
            registry.oppdaterAdressebeskyttetStatus("1", STRENGT_FORTROLIG)
        }
        verify(exactly = 1) {
            registry.oppdaterAdressebeskyttetStatus("2", UGRADERT)
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
