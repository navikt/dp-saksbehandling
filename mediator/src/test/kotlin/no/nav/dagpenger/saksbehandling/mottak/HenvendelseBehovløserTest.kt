package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType.KLAGE
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.henvendelse.HenvendelseMediator
import no.nav.dagpenger.saksbehandling.henvendelse.HåndterHenvendelseResultat
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class HenvendelseBehovløserTest {
    private val testIdentMedSak = "12345612345"
    private val testIdentUtenSak = "11111155555"
    private val sakId = UUIDv7.ny()
    private val søknadIdSomSkalVarsles = UUIDv7.ny()
    private val søknadIdSomIkkeSkalVarsles = UUIDv7.ny()
    private val klageOppgave = TestHelper.lagOppgave(behandling = TestHelper.lagBehandling(utløstAvType = KLAGE))
    private val testRapid = TestRapid()
    private val sakMediatorMock =
        mockk<SakMediator>().also {
            coEvery { it.finnSisteSakId(testIdentMedSak) } returns sakId
            coEvery { it.finnSisteSakId(testIdentUtenSak) } returns null
            coEvery { it.finnSakIdForSøknad(søknadIdSomIkkeSkalVarsles) } returns sakId
        }
    private val klageMediatorMock =
        mockk<KlageMediator>().also {
            coEvery { it.opprettKlage(any()) } returns klageOppgave
        }
    private val oppgaveMediatorMock =
        mockk<OppgaveMediator>().also {
            coEvery { it.skalEttersendingTilSøknadVarsles(søknadIdSomSkalVarsles, any()) } returns true
            coEvery { it.skalEttersendingTilSøknadVarsles(søknadIdSomIkkeSkalVarsles, any()) } returns false
        }
    private val henvendelseMediatorMock =
        mockk<HenvendelseMediator>().also {
            coEvery {
                it.taImotHenvendelse(
                    HenvendelseMottattHendelse(
                        ident = testIdentMedSak,
                        journalpostId = any(),
                        registrertTidspunkt = any(),
                        søknadId = any(),
                        skjemaKode = any(),
                        kategori = any(),
                        utførtAv = any(),
                    ),
                )
            } returns HåndterHenvendelseResultat.HåndtertHenvendelse(sakId)
            coEvery {
                it.taImotHenvendelse(
                    HenvendelseMottattHendelse(
                        ident = testIdentUtenSak,
                        journalpostId = any(),
                        registrertTidspunkt = any(),
                        søknadId = any(),
                        skjemaKode = any(),
                        kategori = any(),
                        utførtAv = any(),
                    ),
                )
            } returns HåndterHenvendelseResultat.UhåndtertHenvendelse
        }

    init {
        HenvendelseBehovløser(
            rapidsConnection = testRapid,
            sakMediator = sakMediatorMock,
            klageMediator = klageMediatorMock,
            oppgaveMediator = oppgaveMediatorMock,
            henvendelseMediator = henvendelseMediatorMock,
        )
    }

    @Test
    fun `Skal motta og håndtere henvendelse om ettersending når vi har en sak for personen - skal varsle ettersending`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentMedSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentMedSak,
                    kategori = "ETTERSENDING",
                    søknadId = søknadIdSomSkalVarsles,
                ),
        )
        verify(exactly = 1) {
            oppgaveMediatorMock.skalEttersendingTilSøknadVarsles(
                søknadId = søknadIdSomSkalVarsles,
                ident = testIdentMedSak,
            )
        }
        // TODO hvordan teste at vi har "varslet"
        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentMedSak",
              "kategori" : "ETTERSENDING",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "sakId" : "$sakId",
                  "håndtert" : true
                  }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og håndtere henvendelse om ettersending når vi har en sak for personen - skal ikke varsle ettersending`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentMedSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentMedSak,
                    kategori = "ETTERSENDING",
                    søknadId = søknadIdSomIkkeSkalVarsles,
                ),
        )
        verify(exactly = 1) {
            oppgaveMediatorMock.skalEttersendingTilSøknadVarsles(
                søknadId = søknadIdSomIkkeSkalVarsles,
                ident = testIdentMedSak,
            )
        }
        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentMedSak",
              "kategori" : "ETTERSENDING",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "sakId" : "$sakId",
                  "håndtert" : true
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta men ikke håndtere henvendelse om ettersending når vi ikke har en sak for personen - skal varsle ettersending`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentUtenSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentUtenSak,
                    kategori = "ETTERSENDING",
                    søknadId = søknadIdSomSkalVarsles,
                ),
        )
        verify(exactly = 1) {
            oppgaveMediatorMock.skalEttersendingTilSøknadVarsles(
                søknadId = søknadIdSomSkalVarsles,
                ident = testIdentUtenSak,
            )
        }
        // TODO hvordan teste at vi har "varslet"
        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentUtenSak",
              "kategori" : "ETTERSENDING",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "håndtert" : false
                }
              },
                "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta men ikke håndtere henvendelse om ettersending når vi ikke har en sak for personen - skal ikke varsle ettersending`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentUtenSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentUtenSak,
                    kategori = "ETTERSENDING",
                    søknadId = søknadIdSomIkkeSkalVarsles,
                ),
        )
        verify(exactly = 1) {
            oppgaveMediatorMock.skalEttersendingTilSøknadVarsles(
                søknadId = søknadIdSomIkkeSkalVarsles,
                ident = testIdentUtenSak,
            )
        }
        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentUtenSak",
              "kategori" : "ETTERSENDING",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "håndtert" : false
                }
              },
                "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og håndtere henvendelse om klage når vi har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentMedSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentMedSak,
                    kategori = "KLAGE",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentMedSak",
              "kategori" : "KLAGE",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "sakId" : "$sakId",
                  "håndtert" : true
                }
              },
                "@final": true
              
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og ikke håndtere henvendelse om klage når vi ikke har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentUtenSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentUtenSak,
                    kategori = "KLAGE",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "behov",
              "@behov": [
                "HåndterHenvendelse"
              ],
              "journalpostId": "$journalpostId",
              "fødselsnummer": "$testIdentUtenSak",
              "registrertDato": "2024-01-01T12:00:00",
              "skjemaKode": "todoFixMe",
              "kategori": "KLAGE",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "håndtert": false
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og håndtere henvendelse om anke når vi har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentMedSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentMedSak,
                    kategori = "ANKE",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentMedSak",
              "registrertDato": "2024-01-01T12:00:00",
              "skjemaKode": "todoFixMe",
              "kategori" : "ANKE",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "sakId" : "$sakId",
                  "håndtert" : true
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og ikke håndtere henvendelse om anke når vi ikke har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentUtenSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentUtenSak,
                    kategori = "ANKE",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentUtenSak",
              "registrertDato": "2024-01-01T12:00:00",
              "skjemaKode": "todoFixMe",
              "kategori" : "ANKE",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "håndtert" : false
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og håndtere henvendelse om utdanning når vi har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentMedSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentMedSak,
                    kategori = "UTDANNING",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentMedSak",
              "kategori" : "UTDANNING",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "sakId" : "$sakId",
                  "håndtert" : true
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og ikke håndtere henvendelse om utdanning når vi ikke har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentUtenSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentUtenSak,
                    kategori = "UTDANNING",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "behov",
              "@behov": [
                "HåndterHenvendelse"
              ],
              "journalpostId": "$journalpostId",
              "fødselsnummer": "$testIdentUtenSak",
              "kategori": "UTDANNING",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "håndtert": false
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og håndtere henvendelse om etablering når vi har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentMedSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentMedSak,
                    kategori = "ETABLERING",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentMedSak",
              "kategori" : "ETABLERING",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "sakId" : "$sakId",
                  "håndtert" : true
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og ikke håndtere henvendelse om etablering når vi ikke har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentUtenSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentUtenSak,
                    kategori = "ETABLERING",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "behov",
              "@behov": [
                "HåndterHenvendelse"
              ],
              "journalpostId": "$journalpostId",
              "fødselsnummer": "$testIdentUtenSak",
              "kategori": "ETABLERING",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "håndtert": false
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og håndtere henvendelse om generell innsending når vi har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentMedSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentMedSak,
                    kategori = "GENERELL",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentMedSak",
              "kategori" : "GENERELL",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "sakId" : "$sakId",
                  "håndtert" : true
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og ikke håndtere henvendelse om generell innsending når vi ikke har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentUtenSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentUtenSak,
                    kategori = "GENERELL",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "behov",
              "@behov": [
                "HåndterHenvendelse"
              ],
              "journalpostId": "$journalpostId",
              "fødselsnummer": "$testIdentUtenSak",
              "kategori": "GENERELL",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "håndtert": false
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og håndtere henvendelse med ukjent skjemakode når vi har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentMedSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentMedSak,
                    kategori = "UKJENT_SKJEMA_KODE",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdentMedSak",
              "kategori" : "UKJENT_SKJEMA_KODE",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "sakId" : "$sakId",
                  "håndtert" : true
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    @Test
    fun `Skal motta og ikke håndtere henvendelse med ukjent skjemakode når vi ikke har en sak for personen`() {
        val journalpostId = "1234"
        testRapid.sendTestMessage(
            key = testIdentUtenSak,
            message =
                håndterHenvendelseBehov(
                    journalpostId = journalpostId,
                    ident = testIdentUtenSak,
                    kategori = "UKJENT_SKJEMA_KODE",
                ),
        )
        testRapid.inspektør.size shouldBe 1

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "behov",
              "@behov": [
                "HåndterHenvendelse"
              ],
              "journalpostId": "$journalpostId",
              "fødselsnummer": "$testIdentUtenSak",
              "kategori": "UKJENT_SKJEMA_KODE",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "håndtert": false
                }
              },
              "@final": true
            }
            """.trimIndent()
    }

    private fun håndterHenvendelseBehov(
        journalpostId: String,
        ident: String,
        kategori: String,
        søknadId: UUID? = null,
        skjemaKode: String = "NAV 04-07.08",
    ) = when (søknadId == null) {
        true ->
            //language=JSON
            """
            {
              "@event_name" : "behov",
              "@behovId" : "${UUIDv7.ny()}",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$ident",
              "kategori" : "$kategori",
              "skjemaKode" : "$skjemaKode",
              "registrertDato" : "${LocalDateTime.now()}"
            }
            """.trimIndent()

        false ->
            //language=JSON
            """
            {
              "@event_name" : "behov",
              "@behovId" : "${UUIDv7.ny()}",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$ident",
              "søknadId" : "$søknadId",
              "kategori" : "$kategori",
              "skjemaKode" : "$skjemaKode",
              "registrertDato" : "2024-01-01T12:00:00"
            }
            """.trimIndent()
    }
}
