package no.nav.dagpenger.saksbehandling.saksbehandler

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.getSnapShot
import org.junit.jupiter.api.Test

internal class CachedSaksbehandlerOppslagTest {
    private val behandlerDTO =
        BehandlerDTO(
            ident = "navIdent",
            fornavn = "vitae",
            etternavn = "quaestio",
            enhet =
                BehandlerDTOEnhetDTO(
                    navn = "lorem",
                    enhetNr = "theophrastus",
                    postadresse = "pericula",
                ),
        )
    private val ekteOppslag =
        mockk<SaksbehandlerOppslag>().also {
            coEvery { it.hentSaksbehandler("navIdent") } returns behandlerDTO
        }

    @Test
    fun `Skal bruke cache der det er mulig`() {
        CachedSaksbehandlerOppslag(ekteOppslag).let {
            runBlocking {
                (1..10).forEach { _ ->
                    it.hentSaksbehandler("navIdent") shouldBe behandlerDTO
                }
            }
        }
        coVerify(exactly = 1) { ekteOppslag.hentSaksbehandler("navIdent") }
    }

    @Test
    fun `Skal registerer prometheus metrikker`() {
        val registry = PrometheusRegistry()
        CachedSaksbehandlerOppslag(ekteOppslag, registry).let {
            (1..10).forEach { _ ->
                runBlocking {
                    it.hentSaksbehandler("navIdent")
                }
            }

            registry.getSnapShot<CounterSnapshot> { it == "dp_saksbehandling_saksbehandler_oppslag_cache" }.let { snapshot ->
                snapshot.dataPoints.single { it.labels["treff"] == "hit" }.value shouldBe 9.0
                snapshot.dataPoints.single { it.labels["treff"] == "miss" }.value shouldBe 1.0
            }
        }
    }
}
