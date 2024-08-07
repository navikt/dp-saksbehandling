package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.prometheus.client.CollectorRegistry
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AdressebeskyttelseKonsumererTest {
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

        val adressebeskyttelseKonsumerer =
            AdressebeskyttelseKonsumerer(
                repository = registry,
                pdlKlient = pdlKlient,
                registry = CollectorRegistry.defaultRegistry,
            )
        adressebeskyttelseKonsumerer.oppdaterAdressebeskyttelseStatus("1", setOf("2", "1"))

        verify(exactly = 1) {
            registry.oppdaterAdressebeskyttetStatus("1", STRENGT_FORTROLIG)
        }
        verify(exactly = 1) {
            registry.oppdaterAdressebeskyttetStatus("2", UGRADERT)
        }
    }

    private fun testPersonResultat(
        fnr: String,
        adresseBeskyttelseGradering: AdresseBeskyttelseGradering,
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
