package no.nav.dagpenger.saksbehandling.tilbakekreving

import io.kotest.matchers.shouldBe
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class PostgresTilbakekrevingRepositoryTest {
    @Test
    fun `Skal kunne hente og lagre tilbakrevinger`() {
        val now = LocalDate.now()
        val tilbakekreving =
            TilbakekrevingHendelse.Tilbakekreving(
                behandlingId = UUID.randomUUID(),
                opprettet = LocalDateTime.now(),
                avventBehandlingTilDato = now,
                varselSendt = now,
                behandlingsstatus = TilbakekrevingHendelse.BehandlingStatus.TIL_FORHÅNDSVARSEL,
                forrigeBehandlingsstatus = TilbakekrevingHendelse.BehandlingStatus.TIL_FORHÅNDSVARSEL,
                totaltFeilutbetaltBeløp = BigDecimal(100),
                saksbehandlingURL = "http://www.bing.com/search?q=laudem",
                fullstendigPeriode =
                    TilbakekrevingHendelse.Periode(
                        fom = now,
                        tom = now.plusDays(2),
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
