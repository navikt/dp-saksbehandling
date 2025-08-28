package no.nav.dagpenger.saksbehandling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
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
        val forslagTilVedtakHendelse =
            ForslagTilVedtakHendelse(
                ident = "12345678910",
                behandletHendelseId = UUIDv7.ny().toString(),
                behandletHendelseType = "Søknad",
                behandlingId = behandlingId,
                emneknagger = emptySet(),
            )
        val sakHistorikk =
            SakHistorikk(
                person =
                    Person(
                        id = UUIDv7.ny(),
                        ident = forslagTilVedtakHendelse.ident,
                        skjermesSomEgneAnsatte = false,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                    ),
                saker =
                    mutableSetOf(
                        Sak(
                            sakId = UUIDv7.ny(),
                            søknadId = UUIDv7.ny(),
                            opprettet = LocalDateTime.now(),
                            behandlinger = mutableSetOf(),
                        ),
                    ),
            )
        OppgaveMediator(
            oppgaveRepository = mockk(),
            behandlingKlient = mockk(),
            utsendingMediator = mockk(),
            sakMediator =
                mockk<SakMediator>().also {
                    every { it.finnSakHistorikkk(any()) } returns sakHistorikk
                },
        ).also { it.setRapidsConnection(rapid) }.let { oppgaveMediator ->
            oppgaveMediator.opprettEllerOppdaterOppgave(forslagTilVedtakHendelse = forslagTilVedtakHendelse)
            rapid.inspektør.size shouldBe 1
            rapid.inspektør.message(0).forventetAlert(behandlingId) shouldBe true
        }
    }

    @Test
    fun `Skal ikke sende alert på rapid dersom behandling finnes`() {
        val behandlingId = UUIDv7.ny()
        val sakId = UUIDv7.ny()
        val opprettet = LocalDateTime.now()
        val forslagTilVedtakHendelse =
            ForslagTilVedtakHendelse(
                ident = "12345678910",
                behandletHendelseId = UUIDv7.ny().toString(),
                behandletHendelseType = "Søknad",
                behandlingId = behandlingId,
                emneknagger = emptySet(),
            )
        val sakHistorikk =
            SakHistorikk(
                person =
                    Person(
                        id = UUIDv7.ny(),
                        ident = forslagTilVedtakHendelse.ident,
                        skjermesSomEgneAnsatte = false,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                    ),
                saker =
                    mutableSetOf(
                        Sak(
                            sakId = sakId,
                            søknadId = UUIDv7.ny(),
                            opprettet = opprettet,
                            behandlinger =
                                mutableSetOf(
                                    Behandling(
                                        behandlingId = behandlingId,
                                        opprettet = LocalDateTime.now(),
                                        hendelse =
                                            BehandlingOpprettetHendelse(
                                                behandlingId = behandlingId,
                                                ident = forslagTilVedtakHendelse.ident,
                                                sakId = sakId,
                                                opprettet = opprettet,
                                                type = BehandlingType.RETT_TIL_DAGPENGER,
                                                utførtAv = Applikasjon("test"),
                                            ),
                                        type = BehandlingType.RETT_TIL_DAGPENGER,
                                    ),
                                ),
                        ),
                    ),
            )

        OppgaveMediator(
            oppgaveRepository =
                mockk<OppgaveRepository>().also {
                    every { it.finnOppgaveFor(behandlingId = any()) } returns null
                    every { it.lagre(oppgave = any()) } just runs
                },
            behandlingKlient = mockk(),
            utsendingMediator = mockk(),
            sakMediator =
                mockk<SakMediator>().also {
                    every { it.finnSakHistorikkk(any()) } returns sakHistorikk
                },
        ).also { it.setRapidsConnection(rapid) }.let { oppgaveMediator ->
            oppgaveMediator.opprettEllerOppdaterOppgave(forslagTilVedtakHendelse = forslagTilVedtakHendelse)
            rapid.inspektør.size shouldBe 0
        }

//        val behandlingId = UUIDv7.ny()
//        OppgaveMediator(
//            oppgaveRepository =
//                mockk<OppgaveRepository>().also {
//                    every { it.finnOppgaveFor(behandlingId = any()) } returns
//                        Oppgave.rehydrer(
//                            oppgaveId = UUIDv7.ny(),
//                            opprettet = LocalDateTime.now(),
//                            emneknagger = emptySet(),
//                            tilstand = Oppgave.KlarTilBehandling,
//                            behandlingId = behandlingId,
//                            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
//                            behandlerIdent = null,
//                            utsattTil = null,
//                            person =
//                                Person(
//                                    ident = "12345678910",
//                                    skjermesSomEgneAnsatte = false,
//                                    adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
//                                ),
//                            meldingOmVedtak =
//                                Oppgave.MeldingOmVedtak(
//                                    kilde = DP_SAK,
//                                    kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
//                                ),
//                        )
//                    every { it.lagre(any()) } just runs
//                },
//            behandlingKlient = mockk(),
//            utsendingMediator = mockk(),
//            sakMediator =
//                mockk<SakMediator>().also {
//                    every { it.finnSakHistorikkk(any()) } returns null
//                },
//        ).also { it.setRapidsConnection(rapid) }.let { oppgaveMediator ->
//
//            oppgaveMediator.opprettEllerOppdaterOppgave(
//                ForslagTilVedtakHendelse(
//                    ident = "12345678910",
//                    behandletHendelseId = UUIDv7.ny().toString(),
//                    behandletHendelseType = "Søknad",
//                    behandlingId = behandlingId,
//                    emneknagger = emptySet(),
//                ),
//            )
//            rapid.inspektør.size shouldBe 0
//        }
    }

    private fun JsonNode.forventetAlert(behandlingId: UUID): Boolean {
        this["@event_name"].asText() shouldBe "saksbehandling_alert"
        this["alertType"].asText() shouldBe "BEHANDLING_IKKE_FUNNET"
        this["feilMelding"].asText() shouldBe "Behandling ikke funnet"
        this["utvidetFeilMelding"].asText() shouldContain behandlingId.toString()
        return true
    }
}
