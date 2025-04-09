package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Notat
import no.nav.dagpenger.saksbehandling.Oppgave.Opprettet
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OppgaveHistorikkDTOMapperTest {
    private val enhet =
        BehandlerDTOEnhetDTO(
            navn = "Enhet",
            enhetNr = "1234",
            postadresse = "Postadresse",
        )

    @Test
    fun `lage historikk for notat`(): Unit =
        runBlocking {
            val beslutter =
                Saksbehandler(
                    navIdent = "beslutterIdent",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER, BESLUTTER),
                )
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
                                skrevetAv = beslutter.navIdent,
                            )
                    },
                saksbehandlerOppslag =
                    mockk<SaksbehandlerOppslag>().also {
                        coEvery { it.hentSaksbehandler(beslutter.navIdent) } returns
                            BehandlerDTO(
                                ident = beslutter.navIdent,
                                fornavn = "fornavn",
                                etternavn = "etternavn",
                                enhet = enhet,
                            )
                    },
            ).let { mapper ->

                val historikk =
                    mapper.lagOppgaveHistorikk(
                        tilstandslogg =
                            Tilstandslogg().also {
                                it.leggTil(
                                    nyTilstand = UNDER_KONTROLL,
                                    hendelse =
                                        SettOppgaveAnsvarHendelse(
                                            oppgaveId = oppgaveId,
                                            ansvarligIdent = beslutter.navIdent,
                                            utførtAv = beslutter,
                                        ),
                                )
                            },
                    )

                historikk.size shouldBe 2
                historikk.first().tittel shouldBe "Notat"
                historikk.last().tittel shouldBe "Under kontroll"

                objectMapper.writeValueAsString(historikk) shouldEqualSpecifiedJsonIgnoringOrder """
                [
                    {
                        "type": "statusendring",
                        "tittel": "Under kontroll",
                        "behandler": {
                            "navn": "fornavn etternavn"
                        }
                    },
                    {
                        "type": "notat",
                        "tidspunkt": "2024-11-01T09:50:00",
                        "tittel": "Notat",
                        "behandler": {
                            "navn": "fornavn etternavn"
                        },
                        "body": "Dette er et notat"
                    }
                ]
            """
            }
        }

    @Test
    fun `Oppgavhistorikk med statusoverganger`() {
        val sistEndretTidspunkt = LocalDateTime.of(2024, 11, 1, 9, 50)
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

            oppgave.tildel(
                settOppgaveAnsvarHendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = beslutter.navIdent,
                        utførtAv = beslutter,
                    ),
            )

            OppgaveHistorikkDTOMapper(
                repository =
                    mockk<OppgaveRepository>(relaxed = true).also {
                        every { it.finnNotat(any()) } returns
                            Notat(
                                notatId = UUIDv7.ny(),
                                tekst = "Dette er et notat",
                                sistEndretTidspunkt = sistEndretTidspunkt,
                                skrevetAv = beslutter.navIdent,
                            )
                    },
                saksbehandlerOppslag =
                    mockk<SaksbehandlerOppslag>().also {
                        coEvery { it.hentSaksbehandler("saksbehandlerIdent") } returns
                            BehandlerDTO(
                                ident = "saksbehandlerIdent",
                                fornavn = "saksbehandlerFornavn",
                                etternavn = "saksbehandlerEtternavn",
                                enhet = enhet,
                            )
                        coEvery { it.hentSaksbehandler("beslutterIdent") } returns
                            BehandlerDTO(
                                ident = "beslutterIdent",
                                fornavn = "beslutterFornavn",
                                etternavn = "beslutterEtternavn",
                                enhet = enhet,
                            )
                    },
            ).let { mapper ->
                val historikk =
                    mapper.lagOppgaveHistorikk(
                        tilstandslogg = oppgave.tilstandslogg,
                    )

                historikk.size shouldBe 5
                historikk.first().tittel shouldBe "Notat"
                objectMapper.writeValueAsString(historikk) shouldEqualSpecifiedJsonIgnoringOrder """
                [
                    {
                        "type": "statusendring",
                        "tittel": "Under kontroll",
                        "behandler": {
                            "navn": "beslutterFornavn beslutterEtternavn"
                        }
                    },
                    {
                        "type": "notat",
                        "tidspunkt": "2024-11-01T09:50:00",
                        "tittel": "Notat",
                        "body": "Dette er et notat",
                        "behandler": {
                            "navn": "beslutterFornavn beslutterEtternavn"
                        }
                    },
                    {
                        "type": "statusendring",
                        "tittel": "Klar til kontroll",
                        "behandler": {
                            "navn": "saksbehandlerFornavn saksbehandlerEtternavn"
                        }
                    },
                    {
                        "type": "statusendring",
                        "tittel": "Under behandling",
                        "behandler": {
                            "navn": "saksbehandlerFornavn saksbehandlerEtternavn"
                        }
                    },
                    {
                        "type": "statusendring",
                        "tittel": "Klar til behandling",
                        "behandler": {
                            "navn": "dp-behandling"
                        }
                    }
                ]
            """
            }
        }
    }
}
