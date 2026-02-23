package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType.MANUELL
import no.nav.dagpenger.saksbehandling.UtløstAvType.MELDEKORT
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTORolleDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTOBehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTOTypeDTO
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
            coEvery { it.person(TestHelper.personIdent) } returns Result.success(TestHelper.pdlPerson)
        }
    private val relevanteJournalpostIdOppslag =
        mockk<RelevanteJournalpostIdOppslag>().also {
            coEvery { it.hentJournalpostIder(any()) } returns setOf("søknadJournalpostId", "vedtakJournalpostId")
        }

    @Test
    fun `Skal mappe og berike oppgaveDTO for tilstand Under kontroll`() {
        val etTidspunkt = LocalDateTime.of(2024, 11, 1, 9, 50)

        runBlocking {
            val oppgave =
                TestHelper.lagOppgave(
                    tilstand = Oppgave.UnderKontroll(),
                    opprettet = etTidspunkt,
                    person = TestHelper.testPerson,
                    tilstandslogg = TestHelper.lagOppgaveTilstandslogg(),
                )

            OppgaveDTOMapper(
                oppslag =
                    Oppslag(
                        pdlKlient,
                        relevanteJournalpostIdOppslag,
                        mockk<SaksbehandlerOppslag>().also {
                            coEvery { it.hentSaksbehandler(TestHelper.saksbehandler.navIdent) } returns
                                BehandlerDTO(
                                    ident = TestHelper.saksbehandler.navIdent,
                                    fornavn = "sbfornavn",
                                    etternavn = "sbetternavn",
                                    enhet =
                                        BehandlerDTOEnhetDTO(
                                            navn = "sbEnhet",
                                            enhetNr = "sbEnhetNr",
                                            postadresse = "sbPostadresse",
                                        ),
                                )
                            coEvery { it.hentSaksbehandler(TestHelper.beslutter.navIdent) } returns
                                BehandlerDTO(
                                    ident = TestHelper.beslutter.navIdent,
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
                    mockk<SakMediator>(),
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
                        "ident": "${TestHelper.personIdent}",
                        "id": "${TestHelper.personId}",
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
                        "statsborgerskap": "NOR"
                      },
                      "tidspunktOpprettet": "2024-11-01T09:50:00",
                      "behandlingType": "RETT_TIL_DAGPENGER",
                      "utlostAv": "SØKNAD",
                      "emneknagger": [],
                      "tilstand": "UNDER_KONTROLL",
                      "lovligeEndringer": {
                        "paaVentAarsaker": [],
                        "avbrytAarsaker": [],
                        "leggTilbakeAarsaker": [
                          "MANGLER_KOMPETANSE",
                          "INHABILITET",
                          "FRAVÆR",
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
                            "navn": "BeslutterNavn",
                            "rolle": "beslutter"
                          }
                        }
                      ],
                      "soknadId": "01953789-f215-744e-9f6e-a55509bae78b",
                      "meldingOmVedtakKilde": "DP_SAK",
                      "kontrollertBrev": "IKKE_RELEVANT"
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
                TestHelper.lagOppgave(
                    tilstand = UnderBehandling,
                    opprettet = etTidspunkt,
                    tilstandslogg = TestHelper.lagOppgaveTilstandslogg(),
                )
            OppgaveDTOMapper(
                oppslag =
                    Oppslag(
                        pdlKlient = pdlKlient,
                        relevanteJournalpostIdOppslag = relevanteJournalpostIdOppslag,
                        saksbehandlerOppslag =
                            mockk<SaksbehandlerOppslag>().also {
                                coEvery { it.hentSaksbehandler(TestHelper.saksbehandler.navIdent) } returns
                                    BehandlerDTO(
                                        ident = TestHelper.saksbehandler.navIdent,
                                        fornavn = "sbfornavn",
                                        etternavn = "sbetternavn",
                                        enhet =
                                            BehandlerDTOEnhetDTO(
                                                navn = "sbEnhet",
                                                enhetNr = "sbEnhetNr",
                                                postadresse = "sbPostadresse",
                                            ),
                                    )
                                coEvery { it.hentSaksbehandler(TestHelper.beslutter.navIdent) } returns
                                    BehandlerDTO(
                                        ident = TestHelper.beslutter.navIdent,
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
                    mockk<SakMediator>(),
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
                        "ident": "${TestHelper.personIdent}",
                        "id": "${TestHelper.personId}",
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
                            "gyldigTom": "${TestHelper.pdlPerson.sikkerhetstiltak.first().gyldigTom}"
                          }
                        ],
                        "statsborgerskap": "NOR"
                      },
                      "tidspunktOpprettet": "2024-11-01T09:50:00",
                      "behandlingType": "RETT_TIL_DAGPENGER",
                      "utlostAv": "SØKNAD",
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
                        ],
                        "avbrytAarsaker": [
                          "BEHANDLES_I_ARENA",
                          "FLERE_SØKNADER",
                          "TRUKKET_SØKNAD",
                          "ANNET"
                        ],
                        "leggTilbakeAarsaker": [
                          "MANGLER_KOMPETANSE",
                          "INHABILITET",
                          "FRAVÆR",
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
                      "soknadId": "01953789-f215-744e-9f6e-a55509bae78b",
                      "meldingOmVedtakKilde": "DP_SAK",
                      "kontrollertBrev": "IKKE_RELEVANT"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Skal mappe og berike oppgaveDTO for manuell behandling`() {
        val etTidspunkt = LocalDateTime.of(2024, 11, 1, 9, 50)
        runBlocking {
            val oppgave =
                TestHelper.lagOppgave(
                    tilstand = UnderBehandling,
                    opprettet = etTidspunkt,
                    behandling = TestHelper.lagBehandling(utløstAvType = MANUELL),
                    tilstandslogg = TestHelper.lagOppgaveTilstandslogg(),
                )
            OppgaveDTOMapper(
                oppslag =
                    Oppslag(
                        pdlKlient = pdlKlient,
                        relevanteJournalpostIdOppslag = relevanteJournalpostIdOppslag,
                        saksbehandlerOppslag =
                            mockk<SaksbehandlerOppslag>().also {
                                coEvery { it.hentSaksbehandler(TestHelper.saksbehandler.navIdent) } returns
                                    BehandlerDTO(
                                        ident = TestHelper.saksbehandler.navIdent,
                                        fornavn = "sbfornavn",
                                        etternavn = "sbetternavn",
                                        enhet =
                                            BehandlerDTOEnhetDTO(
                                                navn = "sbEnhet",
                                                enhetNr = "sbEnhetNr",
                                                postadresse = "sbPostadresse",
                                            ),
                                    )
                                coEvery { it.hentSaksbehandler(TestHelper.beslutter.navIdent) } returns
                                    BehandlerDTO(
                                        ident = TestHelper.beslutter.navIdent,
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
                    mockk<SakMediator>(),
            ).let { mapper ->
                val oppgaveDTO =
                    mapper.lagOppgaveDTO(
                        oppgave,
                    )
                //language=JSON
                objectMapper.writeValueAsString(oppgaveDTO) shouldEqualSpecifiedJsonIgnoringOrder
                    """
                    {
                      "oppgaveId": "${oppgave.oppgaveId}",
                      "behandlingId": "${oppgave.behandling.behandlingId}",
                      "person": {
                        "ident": "${TestHelper.personIdent}",
                        "id": "${TestHelper.personId}",
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
                            "gyldigTom": "${TestHelper.pdlPerson.sikkerhetstiltak.first().gyldigTom}"
                          }
                        ],
                        "statsborgerskap": "NOR"
                      },
                      "tidspunktOpprettet": "2024-11-01T09:50:00",
                      "behandlingType": "RETT_TIL_DAGPENGER",
                      "utlostAv": "MANUELL",
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
                        ],
                        "avbrytAarsaker": [
                          "BEHANDLES_I_ARENA",
                          "FLERE_SØKNADER",
                          "TRUKKET_SØKNAD",
                          "ANNET"
                        ],
                        "leggTilbakeAarsaker": [
                          "MANGLER_KOMPETANSE",
                          "INHABILITET",
                          "FRAVÆR",
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
                      "soknadId": "01953789-f215-744e-9f6e-a55509bae78b",
                      "meldingOmVedtakKilde": "DP_SAK",
                      "kontrollertBrev": "IKKE_RELEVANT"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Skal mappe og berike oppgaveDTO for behandling av meldekort`() {
        val etTidspunkt = LocalDateTime.of(2024, 11, 1, 9, 50)
        runBlocking {
            val oppgave =
                TestHelper.lagOppgave(
                    tilstand = UnderBehandling,
                    opprettet = etTidspunkt,
                    behandling = TestHelper.lagBehandling(utløstAvType = MELDEKORT),
                    tilstandslogg = TestHelper.lagOppgaveTilstandslogg(),
                )
            OppgaveDTOMapper(
                oppslag =
                    Oppslag(
                        pdlKlient = pdlKlient,
                        relevanteJournalpostIdOppslag = relevanteJournalpostIdOppslag,
                        saksbehandlerOppslag =
                            mockk<SaksbehandlerOppslag>().also {
                                coEvery { it.hentSaksbehandler(TestHelper.saksbehandler.navIdent) } returns
                                    BehandlerDTO(
                                        ident = TestHelper.saksbehandler.navIdent,
                                        fornavn = "sbfornavn",
                                        etternavn = "sbetternavn",
                                        enhet =
                                            BehandlerDTOEnhetDTO(
                                                navn = "sbEnhet",
                                                enhetNr = "sbEnhetNr",
                                                postadresse = "sbPostadresse",
                                            ),
                                    )
                                coEvery { it.hentSaksbehandler(TestHelper.beslutter.navIdent) } returns
                                    BehandlerDTO(
                                        ident = TestHelper.beslutter.navIdent,
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
                    mockk<SakMediator>(),
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
                        "ident": "${TestHelper.personIdent}",
                        "id": "${TestHelper.personId}",
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
                            "gyldigTom": "${TestHelper.pdlPerson.sikkerhetstiltak.first().gyldigTom}"
                          }
                        ],
                        "statsborgerskap": "NOR"
                      },
                      "tidspunktOpprettet": "2024-11-01T09:50:00",
                      "behandlingType": "RETT_TIL_DAGPENGER",
                      "utlostAv": "MELDEKORT",
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
                        ],
                        "avbrytAarsaker": [
                          "BEHANDLES_I_ARENA",
                          "FLERE_SØKNADER",
                          "TRUKKET_SØKNAD",
                          "ANNET"
                        ],
                        "leggTilbakeAarsaker": [
                          "MANGLER_KOMPETANSE",
                          "INHABILITET",
                          "FRAVÆR",
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
                      "soknadId": "01953789-f215-744e-9f6e-a55509bae78b",
                      "meldingOmVedtakKilde": "DP_SAK",
                      "kontrollertBrev": "IKKE_RELEVANT"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `lagPersonOversiktDTO skal koble oppgaver til riktig sak via behandlingId`() {
        val behandling1 = TestHelper.lagBehandling(behandlingId = UUIDv7.ny())
        val behandling2 = TestHelper.lagBehandling(behandlingId = UUIDv7.ny())
        val sak1 =
            Sak(
                søknadId = UUIDv7.ny(),
                opprettet = TestHelper.opprettetNå,
            ).also { it.leggTilBehandling(behandling1) }
        val sak2 =
            Sak(
                søknadId = UUIDv7.ny(),
                opprettet = TestHelper.opprettetNå,
            ).also { it.leggTilBehandling(behandling2) }

        val sakHistorikk =
            SakHistorikk.rehydrer(
                person = TestHelper.testPerson,
                saker = setOf(sak1, sak2),
            )

        val oppgave1 = TestHelper.lagOppgave(behandling = behandling1).tilOppgaveOversiktDTO()
        val oppgave2 = TestHelper.lagOppgave(behandling = behandling2).tilOppgaveOversiktDTO()

        val sakMediator =
            mockk<SakMediator>().also {
                every { it.finnSakHistorikk(TestHelper.personIdent) } returns sakHistorikk
            }

        val mapper =
            OppgaveDTOMapper(
                oppslag =
                    Oppslag(
                        pdlKlient,
                        relevanteJournalpostIdOppslag,
                        mockk(relaxed = true),
                        skjermingKlient = mockk(),
                    ),
                oppgaveHistorikkDTOMapper = mockk(relaxed = true),
                sakMediator = sakMediator,
            )

        runBlocking {
            val result = mapper.lagPersonOversiktDTO(TestHelper.testPerson, listOf(oppgave1, oppgave2))

            result.saker shouldHaveSize 2
            val sakDTO1 = result.saker.single { it.id == sak1.sakId }
            val sakDTO2 = result.saker.single { it.id == sak2.sakId }

            sakDTO1.oppgaver shouldHaveSize 1
            sakDTO1.oppgaver.first() shouldBe oppgave1

            sakDTO2.oppgaver shouldHaveSize 1
            sakDTO2.oppgaver.first() shouldBe oppgave2
        }
    }
}
