package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Notat
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Opprettet
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingLåstHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OppgaveHistorikkDTOMapperTest {
    @Test
    fun `lage historikk`(): Unit =
        runBlocking {
            val oppgaveId = UUIDv7.ny()
            val sistEndretTidspunkt = LocalDateTime.of(2024, 11, 1, 9, 50)
            OppgaveHistorikkDTOMapper(
                repository =
                    mockk<OppgaveRepository>(relaxed = true).also {
                        every { it.finnNotat(any()) } returns
                            Notat(
                                notatId = UUIDv7.ny(),
                                tekst = "Dette er et notat",
                                sistEndretTidspunkt = sistEndretTidspunkt,
                            )
                    },
                saksbehandlerOppslag =
                    mockk<SaksbehandlerOppslag>().also {
                        coEvery { it.hentSaksbehandler("saksbehandlerIdent") } returns
                            BehandlerDTO(
                                ident = "saksbehandlerIdent",
                                fornavn = "fornavn",
                                etternavn = "etternavn",
                                enhet = null,
                            )
                    },
            ).let { mapper ->

                val historikk =
                    mapper.lagOppgaveHistorikk(
                        tilstandslogg =
                            Tilstandslogg().also {
                                it.leggTil(
                                    Oppgave.Tilstand.Type.UNDER_KONTROLL,
                                    SettOppgaveAnsvarHendelse(
                                        oppgaveId = oppgaveId,
                                        ansvarligIdent = "ansvarligIdent",
                                        utførtAv =
                                            Saksbehandler(
                                                navIdent = "saksbehandlerIdent",
                                                grupper = emptySet(),
                                                tilganger = setOf(SAKSBEHANDLER, BESLUTTER),
                                            ),
                                    ),
                                )
                            },
                    )

                historikk.size shouldBe 1
                objectMapper.writeValueAsString(historikk) shouldEqualJson """
                [
                    {
                        "type": "notat",
                        "tidspunkt": "2024-11-01T09:50:00",
                        "behandler": {
                            "navn": "fornavn etternavn",
                            "rolle": "beslutter"
                        },
                        "tittel": "Notat",
                        "body": "Dette er et notat"
                    }
                ]
            """
            }
        }

    @Test
    fun `Oppgavhistorikk med statusoverganger`() {
        runBlocking {
            val oppgave =
                lagOppgave(
                    tilstand = Opprettet,
                )
            val saksbehandler =
                Saksbehandler(
                    navIdent = "saksbehandlerIdent",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER),
                )

            val beslutter =
                Saksbehandler(
                    navIdent = "beslutterIdent",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER, BESLUTTER),
                )

            oppgave.oppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = oppgave.behandling.person.ident,
                    søknadId = UUIDv7.ny(),
                    behandlingId = oppgave.behandling.behandlingId,
                ),
            )

            oppgave.tildel(
                settOppgaveAnsvarHendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
            )

            oppgave.sendTilKontroll(
                sendTilKontrollHendelse =
                    SendTilKontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
            )

            oppgave.klarTilKontroll(
                BehandlingLåstHendelse(
                    behandlingId = oppgave.behandling.behandlingId,
                    ident = saksbehandler.navIdent,
                ),
            )

            oppgave.tildel(
                settOppgaveAnsvarHendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = beslutter.navIdent,
                        utførtAv = beslutter,
                    ),
            )

            OppgaveHistorikkDTOMapper(
                repository = mockk<OppgaveRepository>(relaxed = true),
                saksbehandlerOppslag =
                    mockk<SaksbehandlerOppslag>().also {
                        coEvery { it.hentSaksbehandler("saksbehandlerIdent") } returns
                            BehandlerDTO(
                                ident = "saksbehandlerIdent",
                                fornavn = "fornavn",
                                etternavn = "etternavn",
                                enhet = null,
                            )
                        coEvery { it.hentSaksbehandler("beslutterIdent") } returns
                            BehandlerDTO(
                                ident = "beslutterIdent",
                                fornavn = "beslutterFornavn",
                                etternavn = "beslutterEtternavn",
                                enhet = null,
                            )
                    },
            ).let { mapper ->
                val historikk =
                    mapper.lagOppgaveHistorikk(
                        tilstandslogg = oppgave.tilstandslogg,
                    )

                historikk.size shouldBe 5
            }
        }
    }
}
