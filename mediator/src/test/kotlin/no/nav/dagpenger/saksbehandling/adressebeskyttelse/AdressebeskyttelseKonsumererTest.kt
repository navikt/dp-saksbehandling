package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import io.mockk.mockk
import io.mockk.verify
import io.prometheus.client.CollectorRegistry
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering.STRENGT_FORTROLIG
import org.junit.jupiter.api.Test

internal class AdressebeskyttelseKonsumererTest {
    @Test
    fun `test oppdaterAdressebeskyttelseStatus`() {
        val registry = mockk<AdressebeskyttelseRepository>(relaxed = true)

        val adressebeskyttelseKonsumerer =
            AdressebeskyttelseKonsumerer(
                repository = registry,
                registry = CollectorRegistry.defaultRegistry,
            )

        adressebeskyttelseKonsumerer.oppdaterAdressebeskyttelseStatus("12345678901", STRENGT_FORTROLIG)

        verify(exactly = 1) {
            registry.oppdaterAdressebeskyttetStatus("12345678901", STRENGT_FORTROLIG)
        }
    }
}
