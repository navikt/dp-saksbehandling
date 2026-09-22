package no.nav.dagpenger.saksbehandling.tilbakekreving

import io.kotest.matchers.shouldBe
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.tilbakekreving.Tilbakekreving.BehandlingStatus
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class PostgresTilbakekrevingRepositoryTest {
    @Test
    fun `Skal kunne hente og lagre tilbakrevinger`() {
        val today = LocalDate.now()
        val tilbakekreving =
            Tilbakekreving(
                behandlingId = UUID.randomUUID(),
                opprettet = today.atStartOfDay(),
                avventBehandlingTilDato = today,
                varselSendt = today,
                behandlingsstatus = BehandlingStatus.TIL_FORHÅNDSVARSEL,
                forrigeBehandlingsstatus = BehandlingStatus.TIL_FORHÅNDSVARSEL,
                totaltFeilutbetaltBeløp = BigDecimal(100),
                saksbehandlingURL = "http://www.bing.com/search?q=laudem",
                fullstendigPeriode =
                    Tilbakekreving.Periode(
                        fom = today,
                        tom = today.plusDays(2),
                    ),
            )
        DBTestHelper.withMigratedDb { ds ->
            val session = DatabaseSession(ds)
            val repository = PostgresTilbakekrevingRepository(databaseSession = session)
            repository.lagre(
                tilbakekreving =
                tilbakekreving,
                ctx = Transaksjonskontekst.Aktiv(session = sessionOf(ds)),
            )

            repository.hent(tilbakekreving.behandlingId) shouldBe tilbakekreving
        }
    }
}
