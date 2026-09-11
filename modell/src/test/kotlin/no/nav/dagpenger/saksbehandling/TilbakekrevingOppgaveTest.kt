package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.ModellTestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse.BehandlingStatus
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class TilbakekrevingOppgaveTest {
    private val tilbakekrevingBehandlingId = UUIDv7.ny()
    private val eksternBehandlingId = UUIDv7.ny()
    private val saksbehandler = ModellTestHelper.lagSaksbehandler()

    @Test
    fun `Livssyklus test av tilbakekreving med forhåndsvarsel og retur fra kontroll`() {
        val oppgave = lagTilbakekrevingOppgave(OPPRETTET)

        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_FORHÅNDSVARSEL))
        oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING

        oppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler,
            ),
        )

        oppgave.tilstand().type shouldBe UNDER_BEHANDLING

        oppgave.håndter(
            lagTilbakekrevingHendelse(
                status = BehandlingStatus.TIL_BEHANDLING,
                avventBehandlingTilDato = null,
            ),
        )
        oppgave.tilstand().type shouldBe UNDER_BEHANDLING

        oppgave.håndter(
            lagTilbakekrevingHendelse(
                status = BehandlingStatus.TIL_BEHANDLING,
                avventBehandlingTilDato = LocalDate.now().plusDays(10),
            ),
        )
        oppgave.tilstand().type shouldBe PAA_VENT

        oppgave.håndter(
            lagTilbakekrevingHendelse(
                status = BehandlingStatus.TIL_BEHANDLING,
                avventBehandlingTilDato = null,
            ),
        )
        oppgave.tilstand().type shouldBe UNDER_BEHANDLING
        oppgave.emneknagger shouldContain Emneknagg.PåVent.FORHÅNDSVARSEL_FRIST_UTGÅTT.visningsnavn

        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_GODKJENNING))
        oppgave.tilstand().type shouldBe KLAR_TIL_KONTROLL

        val beslutter = Saksbehandler("B654321", emptySet(), setOf(TilgangType.BESLUTTER))
        oppgave.tildel(
            no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter.navIdent,
                utførtAv = beslutter,
            ),
        )
        oppgave.tilstand().type shouldBe UNDER_KONTROLL

        oppgave.håndter(
            lagTilbakekrevingHendelse(
                status = BehandlingStatus.TIL_BEHANDLING,
                avventBehandlingTilDato = null,
            ),
        )
        oppgave.tilstand().type shouldBe UNDER_BEHANDLING
        oppgave.behandlerIdent shouldBe saksbehandler.navIdent
        oppgave.emneknagger shouldContain Emneknagg.Kontroll.RETUR_FRA_KONTROLL.visningsnavn

        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_BEHANDLING))
        oppgave.tilstand().type shouldBe UNDER_BEHANDLING
        oppgave.emneknagger shouldContain Emneknagg.Kontroll.RETUR_FRA_KONTROLL.visningsnavn
        oppgave.behandlerIdent shouldBe saksbehandler.navIdent

        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_GODKJENNING))
        oppgave.tilstand().type shouldBe UNDER_KONTROLL
        oppgave.emneknagger shouldContain Emneknagg.Kontroll.TIDLIGERE_KONTROLLERT.visningsnavn
        oppgave.behandlerIdent shouldBe beslutter.navIdent

        oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.AVSLUTTET))
        oppgave.tilstand().type shouldBe FERDIG_BEHANDLET
    }

    @Test
    fun `TilbakekrevingOppgave må ha behandling utløst av tilbakekreving`() {
        val søknadOppgave = lagOppgave()
        shouldThrow<IllegalArgumentException> {
            søknadOppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.OPPRETTET))
        }
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
    fun `KlarTilBehandling - TilbakekrevingHendelse er ulovlig tilstandsendring`() {
        val oppgave = lagTilbakekrevingOppgave(KLAR_TIL_BEHANDLING)
        shouldThrow<Oppgave.Tilstand.UlovligTilstandsendringException> {
            oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_BEHANDLING))
        }
    }

    @Test
    fun `KlarTilKontroll - TilbakekrevingHendelse er ulovlig tilstandsendring`() {
        val oppgave = lagTilbakekrevingOppgave(KLAR_TIL_KONTROLL)
        shouldThrow<Oppgave.Tilstand.UlovligTilstandsendringException> {
            oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.TIL_BEHANDLING))
        }
    }

    @Test
    fun `FerdigBehandlet - TilbakekrevingHendelse er ulovlig tilstandsendring`() {
        val oppgave = lagTilbakekrevingOppgave(FERDIG_BEHANDLET)
        shouldThrow<Oppgave.Tilstand.UlovligTilstandsendringException> {
            oppgave.håndter(lagTilbakekrevingHendelse(BehandlingStatus.AVSLUTTET))
        }
    }

    private fun lagTilbakekrevingOppgave(tilstand: Oppgave.Tilstand.Type = OPPRETTET) =
        lagOppgave(
            tilstandType = tilstand,
            behandling =
                Behandling(
                    behandlingId = tilbakekrevingBehandlingId,
                    opprettet = LocalDateTime.now(),
                    utløstAv = HendelseBehandler.Intern.Tilbakekreving,
                    hendelse = lagTilbakekrevingHendelse(BehandlingStatus.OPPRETTET),
                ),
        )

    private fun lagTilbakekrevingHendelse(
        status: BehandlingStatus,
        avventBehandlingTilDato: LocalDate? = null,
    ) = TilbakekrevingHendelse(
        ident = "12345678910",
        eksternFagsakId = "100001234",
        eksternBehandlingId = eksternBehandlingId,
        hendelseOpprettet = LocalDateTime.now(),
        tilbakekreving =
            TilbakekrevingHendelse.Tilbakekreving(
                behandlingId = tilbakekrevingBehandlingId,
                opprettet = LocalDateTime.now().minusDays(10),
                avventBehandlingTilDato = avventBehandlingTilDato,
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
