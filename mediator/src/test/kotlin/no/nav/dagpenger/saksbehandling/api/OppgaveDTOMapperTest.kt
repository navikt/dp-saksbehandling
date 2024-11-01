package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Notat
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.BESLUTTER_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.SAKSBEHANDLER_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.TEST_IDENT
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerEnhetDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
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
    private val journalpostIdClient =
        mockk<JournalpostIdClient>().also {
            coEvery { it.hentJournalpostId(any()) } returns Result.success("journalpostId")
        }

    @Test
    fun `Skal mappe og berike oppgaveDTO`() {
        val oppgaveOpprettet = LocalDateTime.of(2024, 11, 1, 9, 50)
        runBlocking {
            val oppgave =
                OppgaveApiTestHelper.lagTestOppgaveMedTilstand(
                    tilstand = Oppgave.Tilstand.Type.UNDER_KONTROLL,
                    oprettet = oppgaveOpprettet,
                )
            OppgaveDTOMapper(
                pdlKlient = pdlKlient,
                journalpostIdClient = journalpostIdClient,
                saksbehandlerOppslag =
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
                repository =
                    mockk<OppgaveRepository>().also {
                        every { it.finnNotat(any()) } returns
                            Notat(
                                notatId = UUIDv7.ny(),
                                tekst = "Dette er et notat",
                                sistEndretTidspunkt = LocalDateTime.of(2021, 1, 1, 1, 1),
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
                      "emneknagger": [
                        "Søknadsbehandling"
                      ],
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
                      "journalpostIder": [],
                      "historikk": [
                        {
                          "type": "notat",
                          "tidspunkt": "2021-01-01T01:01:00",
                          "tittel": "Notat",
                          "body": "Dette er et notat",
                          "behandler": {
                            "navn": "BeslutterIdent",
                            "rolle": "beslutter"
                          }
                        }
                      ]
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `lage historikk`() {
        OppgaveDTOMapper(
            pdlKlient = mockk(relaxed = true),
            journalpostIdClient = mockk(relaxed = true),
            saksbehandlerOppslag = mockk(relaxed = true),
            repository =
                mockk<OppgaveRepository>(relaxed = true).also {
                    every { it.finnNotat(any()) } returns
                        Notat(
                            notatId = UUIDv7.ny(),
                            tekst = "Dette er et notat",
                            sistEndretTidspunkt = LocalDateTime.now(),
                        )
                },
        ).let { mapper ->

            val histtorikkk =
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
                                            navIdent = "utførtAv",
                                            grupper = emptySet(),
                                            tilganger = setOf(SAKSBEHANDLER, BESLUTTER),
                                        ),
                                ),
                            )
                        },
                )

            histtorikkk.size shouldBe 1
        }
    }
}
