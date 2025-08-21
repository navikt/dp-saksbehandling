package no.nav.dagpenger.saksbehandling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class OppgaveMediatorAlertTest {
    private val rapid = TestRapid()

    @Test
    fun `Skal sende alert på rapid dersom behandling ikke finnes`() {
        val behandlingId = UUIDv7.ny()
        OppgaveMediator(
            oppgaveRepository =
                mockk<OppgaveRepository>().also {
                    every { it.finnOppgaveFor(behandlingId = any()) } returns null
                },
            behandlingKlient = mockk(),
            utsendingMediator = mockk(),
            sakMediator =
                mockk<SakMediator>().also {
                    every { it.finnSakHistorikkk(any()) } returns null
                },
        ).also { it.setRapidsConnection(rapid) }.let { oppgaveMediator ->

            oppgaveMediator.opprettEllerOppdaterOppgave(
                ForslagTilVedtakHendelse(
                    ident = "12345678910",
                    behandletHendelseId = UUIDv7.ny().toString(),
                    behandletHendelseType = "Søknad",
                    behandlingId = behandlingId,
                    emneknagger = emptySet(),
                ),
            )
            rapid.inspektør.size shouldBe 1
            rapid.inspektør.message(0).forventetAlert(behandlingId) shouldBe true
        }
    }

    @Test
    fun `Skal ikke sende alert på rapid dersom sak mangler - men oppgave finnes`() {
        val behandlingId = UUIDv7.ny()
        OppgaveMediator(
            oppgaveRepository =
                mockk<OppgaveRepository>().also {
                    every { it.finnOppgaveFor(behandlingId = any()) } returns
                        Oppgave.rehydrer(
                            oppgaveId = UUIDv7.ny(),
                            opprettet = LocalDateTime.now(),
                            emneknagger = emptySet(),
                            tilstand = Oppgave.KlarTilBehandling,
                            behandlingId = behandlingId,
                            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
                            behandlerIdent = null,
                            utsattTil = null,
                            person =
                                Person(
                                    ident = "12345678910",
                                    skjermesSomEgneAnsatte = false,
                                    adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                                ),
                            meldingOmVedtakKilde = DP_SAK,
                        )
                    every { it.lagre(any()) } just runs
                },
            behandlingKlient = mockk(),
            utsendingMediator = mockk(),
            sakMediator =
                mockk<SakMediator>().also {
                    every { it.finnSakHistorikkk(any()) } returns null
                },
        ).also { it.setRapidsConnection(rapid) }.let { oppgaveMediator ->

            oppgaveMediator.opprettEllerOppdaterOppgave(
                ForslagTilVedtakHendelse(
                    ident = "12345678910",
                    behandletHendelseId = UUIDv7.ny().toString(),
                    behandletHendelseType = "Søknad",
                    behandlingId = behandlingId,
                    emneknagger = emptySet(),
                ),
            )
            rapid.inspektør.size shouldBe 0
        }
    }

    private fun JsonNode.forventetAlert(behandlingId: UUID): Boolean {
        this["@event_name"].asText() shouldBe "saksbehandling_alert"
        this["alertType"].asText() shouldBe "BEHANDLING_IKKE_FUNNET"
        this["feilMelding"].asText() shouldBe "Behandling ikke funnet"
        this["utvidetFeilMelding"].asText() shouldContain behandlingId.toString()
        return true
    }
}
