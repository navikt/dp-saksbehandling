package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualJson
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.BESLUTTER_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.SAKSBEHANDLER_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.TEST_IDENT
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerEnhetDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkBehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTO
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OppgaveDTOMapperTest {
    private val oppgaveId = UUIDv7.ny()
    private val pdlKlient =
        mockk<PDLKlient>().also {
            coEvery { it.person(TEST_IDENT) } returns Result.success(OppgaveApiTestHelper.testPerson)
        }
    private val relevanteJournalpostIdOppslag =
        mockk<RelevanteJournalpostIdOppslag>().also {
            coEvery { it.hentJournalpostIder(any()) } returns setOf("søknadJournalpostId", "vedtakJournalpostId")
        }

    @Test
    fun `Skal mappe og berike oppgaveDTO`() {
        val etTidspunkt = LocalDateTime.of(2024, 11, 1, 9, 50)
        runBlocking {
            val oppgave =
                OppgaveApiTestHelper.lagTestOppgaveMedTilstand(
                    tilstand = Oppgave.Tilstand.Type.UNDER_KONTROLL,
                    oprettet = etTidspunkt,
                )
            OppgaveDTOMapper(
                oppslag =
                    Oppslag(
                        pdlKlient,
                        relevanteJournalpostIdOppslag,
                        mockk<SaksbehandlerOppslag>().also {
                            coEvery { it.hentSaksbehandler(SAKSBEHANDLER_IDENT) } returns
                                BehandlerDTO(
                                    ident = SAKSBEHANDLER_IDENT,
                                    fornavn = "sbfornavn",
                                    etternavn = "sbetternavn",
                                    enhet =
                                        BehandlerEnhetDTO(
                                            navn = "sbEnhet",
                                            enhetNr = "sbEnhetNr",
                                            postadresse = "sbPostadresse",
                                        ),
                                )
                            coEvery { it.hentSaksbehandler(BESLUTTER_IDENT) } returns
                                BehandlerDTO(
                                    ident = BESLUTTER_IDENT,
                                    fornavn = "befornavn",
                                    etternavn = "beetternavn",
                                    enhet =
                                        BehandlerEnhetDTO(
                                            navn = "beEnhet",
                                            enhetNr = "beEnhetNr",
                                            postadresse = "bePostadresse",
                                        ),
                                )
                        },
                        skjermingKlient = mockk(),
                    ),
                oppgaveHistorikkDTOMapper =
                    mockk<OppgaveHistorikkDTOMapper>().also {
                        coEvery { it.lagOppgaveHistorikk(oppgave.tilstandslogg) } returns
                            listOf(
                                OppgaveHistorikkDTO(
                                    type = OppgaveHistorikkDTO.Type.notat,
                                    tidspunkt = etTidspunkt,
                                    tittel = "Notat",
                                    body = "Dette er et notat",
                                    behandler =
                                        OppgaveHistorikkBehandlerDTO(
                                            navn = "BeslutterNavn",
                                            rolle = OppgaveHistorikkBehandlerDTO.Rolle.beslutter,
                                        ),
                                ),
                            )
                    },
            ).let { mapper ->
                val oppgaveDTO =
                    mapper.lagOppgaveDTO(
                        oppgave,
                    )
                //language=JSON
                objectMapper.writeValueAsString(oppgaveDTO) shouldEqualJson
                    """
                    {
                      "oppgaveId": "${oppgave.oppgaveId}",
                      "behandlingId": "${oppgave.behandling.behandlingId}",
                      "person": {
                        "ident": "12345612345",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "alder": 0,
                        "kjonn": "UKJENT",
                        "skjermesSomEgneAnsatte": false,
                        "adressebeskyttelseGradering": "UGRADERT",
                        "statsborgerskap": "NOR"
                      },
                      "tidspunktOpprettet": "2024-11-01T09:50:00",
                      "emneknagger": [],
                      "tilstand": "UNDER_KONTROLL",
                      "saksbehandler": {
                        "ident": "SaksbehandlerIdent",
                        "fornavn": "sbfornavn",
                        "etternavn": "sbetternavn",
                        "enhet": {
                          "navn": "sbEnhet",
                          "enhetNr": "sbEnhetNr",
                          "postadresse": "sbPostadresse"
                        }
                      },
                      "beslutter": {
                        "ident": "BeslutterIdent",
                        "fornavn": "befornavn",
                        "etternavn": "beetternavn",
                        "enhet": {
                          "navn": "beEnhet",
                          "enhetNr": "beEnhetNr",
                          "postadresse": "bePostadresse"
                        }
                      },
                      "journalpostIder": [
                        "søknadJournalpostId",
                        "vedtakJournalpostId"
                      ],
                      "historikk": [
                        {
                          "type": "notat",
                          "tidspunkt": "2024-11-01T09:50:00",
                          "tittel": "Notat",
                          "body": "Dette er et notat",
                          "behandler": {
                            "navn": "BeslutterNavn",
                            "rolle": "beslutter"
                          }
                        }
                      ]
                    }
                    """.trimIndent()
            }
        }
    }
}
