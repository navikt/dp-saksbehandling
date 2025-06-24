package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualJson
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.BESLUTTER_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.SAKSBEHANDLER_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.TEST_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.TEST_UUID
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTORolleDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTOBehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTOTypeDTO
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OppgaveDTOMapperTest {
    private val pdlKlient =
        mockk<PDLKlient>().also {
            coEvery { it.person(TEST_IDENT) } returns Result.success(OppgaveApiTestHelper.testPerson)
        }
    private val relevanteJournalpostIdOppslag =
        mockk<RelevanteJournalpostIdOppslag>().also {
            coEvery { it.hentJournalpostIder(any()) } returns setOf("søknadJournalpostId", "vedtakJournalpostId")
        }

    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val sakId = UUIDv7.ny()

    @Test
    fun `Skal mappe og berike oppgaveDTO for tilstand Under kontroll`() {
        val etTidspunkt = LocalDateTime.of(2024, 11, 1, 9, 50)

        runBlocking {
            val oppgave =
                OppgaveApiTestHelper.lagTestOppgaveMedTilstand(
                    tilstand = Oppgave.Tilstand.Type.UNDER_KONTROLL,
                    oprettet = etTidspunkt,
                    behandlingId = behandlingId,
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
                                        BehandlerDTOEnhetDTO(
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
                                        BehandlerDTOEnhetDTO(
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
                                    type = OppgaveHistorikkDTOTypeDTO.NOTAT,
                                    tidspunkt = etTidspunkt,
                                    tittel = "Notat",
                                    body = "Dette er et notat",
                                    behandler =
                                        OppgaveHistorikkDTOBehandlerDTO(
                                            navn = "BeslutterNavn",
                                            rolle = BehandlerDTORolleDTO.BESLUTTER,
                                        ),
                                ),
                            )
                    },
                sakMediator =
                    mockk<SakMediator>().also {
                        coEvery { it.finnSakHistorikkk(ident = oppgave.personIdent()) } returns
                            SakHistorikk(
                                person = oppgave.person,
                                saker =
                                    mutableSetOf(
                                        Sak(
                                            sakId = sakId,
                                            søknadId = søknadId,
                                            opprettet = oppgave.opprettet,
                                            behandlinger =
                                                mutableSetOf(
                                                    Behandling(
                                                        behandlingId = oppgave.behandlingId,
                                                        opprettet = oppgave.opprettet,
                                                        hendelse = TomHendelse,
                                                        type = BehandlingType.RETT_TIL_DAGPENGER,
                                                        oppgaveId = oppgave.oppgaveId,
                                                    ),
                                                ),
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
                      "behandlingId": "${oppgave.behandlingId}",
                      "person": {
                        "ident": "12345612345",
                        "id": "$TEST_UUID",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "alder": 0,
                        "kjonn": "UKJENT",
                        "skjermesSomEgneAnsatte": false,
                        "adressebeskyttelseGradering": "UGRADERT",
                        "sikkerhetstiltak": [
                          {
                            "beskrivelse": "To ansatte i samtale",
                            "gyldigTom": "${LocalDate.now().plusDays(1)}"
                          }
                        ],
                        "saker": [
                          {
                            "id": "$sakId",
                            "behandlinger": [
                              {
                                "id": "$behandlingId",
                                "behandlingType": "RETT_TIL_DAGPENGER",
                                "opprettet": "2024-11-01T09:50:00",
                                "oppgaveId": "${oppgave.oppgaveId}"
                              }
                            ]
                          }
                        ],
                        "oppgaver": [],
                        "statsborgerskap": "NOR"
                      },
                      "tidspunktOpprettet": "2024-11-01T09:50:00",
                      "behandlingType": "RETT_TIL_DAGPENGER",
                      "emneknagger": [],
                      "tilstand": "UNDER_KONTROLL",
                      "lovligeEndringer": {
                        "paaVentAarsaker": []
                      },
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
                      ],
                      "soknadId": "01953789-f215-744e-9f6e-a55509bae78b"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Skal mappe og berike oppgaveDTO for tilstand Under behandling`() {
        val etTidspunkt = LocalDateTime.of(2024, 11, 1, 9, 50)
        runBlocking {
            val oppgave =
                OppgaveApiTestHelper.lagTestOppgaveMedTilstand(
                    tilstand = Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                    oprettet = etTidspunkt,
                    behandlingId = behandlingId,
                )
            OppgaveDTOMapper(
                oppslag =
                    Oppslag(
                        pdlKlient = pdlKlient,
                        relevanteJournalpostIdOppslag = relevanteJournalpostIdOppslag,
                        saksbehandlerOppslag =
                            mockk<SaksbehandlerOppslag>().also {
                                coEvery { it.hentSaksbehandler(SAKSBEHANDLER_IDENT) } returns
                                    BehandlerDTO(
                                        ident = SAKSBEHANDLER_IDENT,
                                        fornavn = "sbfornavn",
                                        etternavn = "sbetternavn",
                                        enhet =
                                            BehandlerDTOEnhetDTO(
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
                                            BehandlerDTOEnhetDTO(
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
                                    type = OppgaveHistorikkDTOTypeDTO.NOTAT,
                                    tidspunkt = etTidspunkt,
                                    tittel = "Notat",
                                    body = "Dette er et notat",
                                    behandler =
                                        OppgaveHistorikkDTOBehandlerDTO(
                                            navn = "SaksbehandlerNavn",
                                            rolle = BehandlerDTORolleDTO.SAKSBEHANDLER,
                                        ),
                                ),
                            )
                    },
                sakMediator =
                    mockk<SakMediator>().also {
                        coEvery { it.finnSakHistorikkk(ident = oppgave.personIdent()) } returns
                            SakHistorikk(
                                person = oppgave.person,
                                saker =
                                    mutableSetOf(
                                        Sak(
                                            sakId = sakId,
                                            søknadId = søknadId,
                                            opprettet = oppgave.opprettet,
                                            behandlinger =
                                                mutableSetOf(
                                                    Behandling(
                                                        behandlingId = oppgave.behandlingId,
                                                        opprettet = oppgave.opprettet,
                                                        hendelse = TomHendelse,
                                                        type = BehandlingType.RETT_TIL_DAGPENGER,
                                                        oppgaveId = oppgave.oppgaveId,
                                                    ),
                                                ),
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
                      "behandlingId": "${oppgave.behandlingId}",
                      "person": {
                        "ident": "12345612345",
                        "id": "$TEST_UUID",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "alder": 0,
                        "kjonn": "UKJENT",
                        "skjermesSomEgneAnsatte": false,
                        "adressebeskyttelseGradering": "UGRADERT",
                        "sikkerhetstiltak": [
                          {
                            "beskrivelse": "To ansatte i samtale",
                            "gyldigTom": "${OppgaveApiTestHelper.testPerson.sikkerhetstiltak.first().gyldigTom}"
                          }
                        ],
                        "saker": [
                          {
                            "id": "$sakId",
                            "behandlinger": [
                              {
                                "id": "$behandlingId",
                                "behandlingType": "RETT_TIL_DAGPENGER",
                                "opprettet": "2024-11-01T09:50:00",
                                "oppgaveId": "${oppgave.oppgaveId}"
                              }
                            ]
                          }
                        ],
                        "oppgaver": [],
                        "statsborgerskap": "NOR"
                      },
                      "tidspunktOpprettet": "2024-11-01T09:50:00",
                      "behandlingType": "RETT_TIL_DAGPENGER",
                      "emneknagger": [],
                      "tilstand": "UNDER_BEHANDLING",
                      "lovligeEndringer": {
                        "paaVentAarsaker": [
                          "AVVENT_SVAR",
                          "AVVENT_DOKUMENTASJON",
                          "AVVENT_MELDEKORT",
                          "AVVENT_PERMITTERINGSÅRSAK",
                          "AVVENT_RAPPORTERINGSFRIST",
                          "AVVENT_SVAR_PÅ_FORESPØRSEL",
                          "ANNET"
                        ]
                      },
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
                            "navn": "SaksbehandlerNavn",
                            "rolle": "saksbehandler"
                          }
                        }
                      ],
                      "soknadId": "01953789-f215-744e-9f6e-a55509bae78b"
                    }
                    """.trimIndent()
            }
        }
    }
}
