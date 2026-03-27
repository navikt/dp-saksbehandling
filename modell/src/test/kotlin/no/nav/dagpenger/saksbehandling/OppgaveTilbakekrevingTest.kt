package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.ModellTestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse.BehandlingStatus
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class OppgaveTilbakekrevingTest {
    private val tilbakekrevingBehandlingId = UUIDv7.ny()
    private val eksternBehandlingId = UUIDv7.ny()

    @Test
    fun `Opprettet - TIL_BEHANDLING skal endre tilstand til KlarTilBehandling`() {
        val oppgave = lagOppgave(OPPRETTET)
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_BEHANDLING))
        oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING
    }

    @Test
    fun `UnderBehandling - TIL_GODKJENNING uten tidligere beslutter skal gi KlarTilKontroll`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING)
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_GODKJENNING))
        oppgave.tilstand().type shouldBe KLAR_TIL_KONTROLL
        oppgave.behandlerIdent shouldBe null
    }

    @Test
    fun `UnderKontroll - AVSLUTTET skal gi FerdigBehandlet`() {
        val oppgave = lagOppgave(UNDER_KONTROLL)
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.AVSLUTTET))
        oppgave.tilstand().type shouldBe FERDIG_BEHANDLET
    }

    @Test
    fun `UnderKontroll - TIL_BEHANDLING (underkjent) skal gi UnderBehandling med emneknagger`() {
        val oppgave = lagOppgave(UNDER_KONTROLL)
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_BEHANDLING))
        oppgave.tilstand().type shouldBe UNDER_BEHANDLING
        oppgave.emneknagger shouldContain "Retur fra kontroll"
        oppgave.emneknagger shouldNotContain "Tidligere kontrollert"
    }

    @Test
    fun `Full livssyklus - OPPRETTET til AVSLUTTET`() {
        val oppgave = lagOppgave(OPPRETTET)

        // TIL_BEHANDLING: Opprettet → KlarTilBehandling
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_BEHANDLING))
        oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING

        // Saksbehandler tildeler seg oppgaven → UnderBehandling
        val saksbehandler = Saksbehandler("Z123456", emptySet(), setOf(TilgangType.SAKSBEHANDLER))
        oppgave.tildel(
            no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler,
            ),
        )
        oppgave.tilstand().type shouldBe UNDER_BEHANDLING

        // TIL_GODKJENNING: UnderBehandling → KlarTilKontroll
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_GODKJENNING))
        oppgave.tilstand().type shouldBe KLAR_TIL_KONTROLL

        // Beslutter tildeler seg → UnderKontroll
        val beslutter = Saksbehandler("B654321", emptySet(), setOf(TilgangType.BESLUTTER))
        oppgave.tildel(
            no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter.navIdent,
                utførtAv = beslutter,
            ),
        )
        oppgave.tilstand().type shouldBe UNDER_KONTROLL

        // AVSLUTTET: UnderKontroll → FerdigBehandlet
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.AVSLUTTET))
        oppgave.tilstand().type shouldBe FERDIG_BEHANDLET
    }

    @Test
    fun `Full livssyklus med underkjenning`() {
        val oppgave = lagOppgave(OPPRETTET)
        val saksbehandler = Saksbehandler("Z123456", emptySet(), setOf(TilgangType.SAKSBEHANDLER))
        val beslutter = Saksbehandler("B654321", emptySet(), setOf(TilgangType.BESLUTTER))

        // TIL_BEHANDLING: Opprettet → KlarTilBehandling
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_BEHANDLING))
        oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING

        // Saksbehandler tildeler seg → UnderBehandling
        oppgave.tildel(
            no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler,
            ),
        )
        oppgave.tilstand().type shouldBe UNDER_BEHANDLING

        // TIL_GODKJENNING: UnderBehandling → KlarTilKontroll
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_GODKJENNING))
        oppgave.tilstand().type shouldBe KLAR_TIL_KONTROLL

        // Beslutter tildeler seg → UnderKontroll
        oppgave.tildel(
            no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter.navIdent,
                utførtAv = beslutter,
            ),
        )
        oppgave.tilstand().type shouldBe UNDER_KONTROLL

        // Beslutter underkjenner: TIL_BEHANDLING → UnderBehandling
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_BEHANDLING))
        oppgave.tilstand().type shouldBe UNDER_BEHANDLING
        oppgave.emneknagger shouldContain "Retur fra kontroll"
        oppgave.behandlerIdent shouldBe saksbehandler.navIdent

        // TIL_GODKJENNING igjen: UnderBehandling → UnderKontroll (beslutter finnes fra forrige runde)
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_GODKJENNING))
        oppgave.tilstand().type shouldBe UNDER_KONTROLL
        oppgave.behandlerIdent shouldBe beslutter.navIdent

        // AVSLUTTET: UnderKontroll → FerdigBehandlet
        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.AVSLUTTET))
        oppgave.tilstand().type shouldBe FERDIG_BEHANDLET
    }

    @Test
    fun `UnderBehandling - OPPRETTET er ulovlig tilstandsendring`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING)
        shouldThrow<IllegalArgumentException> {
            oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.OPPRETTET))
        }
    }

    @Test
    fun `UnderBehandling - AVSLUTTET er ulovlig tilstandsendring`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING)
        shouldThrow<IllegalArgumentException> {
            oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.AVSLUTTET))
        }
    }

    @Test
    fun `KlarTilBehandling - tilbakekrevingHendelse er ulovlig tilstandsendring`() {
        val oppgave = lagOppgave(KLAR_TIL_BEHANDLING)
        shouldThrow<Oppgave.Tilstand.UlovligTilstandsendringException> {
            oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_BEHANDLING))
        }
    }

    @Test
    fun `FerdigBehandlet - tilbakekrevingHendelse er ulovlig tilstandsendring`() {
        val oppgave = lagOppgave(FERDIG_BEHANDLET)
        shouldThrow<Oppgave.Tilstand.UlovligTilstandsendringException> {
            oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.AVSLUTTET))
        }
    }

    private fun lagTilbakekrevingHendelse(status: BehandlingStatus) =
        TilbakekrevingHendelse(
            ident = "12345678910",
            eksternFagsakId = "100001234",
            eksternBehandlingId = eksternBehandlingId,
            hendelseOpprettet = LocalDateTime.now(),
            tilbakekreving =
                TilbakekrevingHendelse.Tilbakekreving(
                    behandlingId = tilbakekrevingBehandlingId,
                    sakOpprettet = LocalDateTime.now().minusDays(10),
                    varselSendt = LocalDate.now().minusDays(5),
                    behandlingsstatus = status,
                    forrigeBehandlingsstatus = null,
                    totaltFeilutbetaltBeløp = BigDecimal("25000"),
                    saksbehandlingURL = "https://tilbakekreving.intern.nav.no/behandling/$tilbakekrevingBehandlingId",
                    fullstendigPeriode =
                        TilbakekrevingHendelse.Periode(
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 6, 30),
                        ),
                ),
        )
}
