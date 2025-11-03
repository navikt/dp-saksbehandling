package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.henvendelse.HenvendelseMediator
import no.nav.dagpenger.saksbehandling.henvendelse.HåndterHenvendelseResultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class HenvendelseBehovløserTest {
    private val testIdent = "12345612345"
    private val sakId = UUIDv7.ny()
    private val søknadId = UUIDv7.ny()
    private val testRapid = TestRapid()
    private val journalPostId = "123123"
    private val nå = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `Skal parse behov riktig og svare med riktig løsning der vi eier saken`() {
        val slot = slot<HenvendelseMottattHendelse>()
        HenvendelseBehovløser(
            rapidsConnection = testRapid,
            henvendelseMediator =
                mockk<HenvendelseMediator>(relaxed = true).also {
                    every { it.taImotHenvendelse(capture(slot)) } returns
                        HåndterHenvendelseResultat.HåndtertHenvendelse(
                            sakId,
                        )
                },
        )
        testRapid.sendTestMessage(
            key = testIdent,
            message =
                håndterHenvendelseBehov(
                    ident = testIdent,
                    journalpostId = journalPostId,
                    registrertTidspunkt = nå,
                    søknadId = søknadId,
                    skjemaKode = "NAVe",
                    kategori = Kategori.ETTERSENDING,
                ),
        )

        slot.captured.let {
            it.ident shouldBe testIdent
            it.journalpostId shouldBe journalPostId
            it.registrertTidspunkt shouldBe nå
            it.søknadId shouldBe søknadId
            it.skjemaKode shouldBe "NAVe"
            it.kategori shouldBe Kategori.ETTERSENDING
        }

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalPostId",
              "fødselsnummer" : "$testIdent",
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
    fun `Skal parse behov riktig og svare med riktig løsning der vi IKKE eier saken`() {
        val slot = slot<HenvendelseMottattHendelse>()
        HenvendelseBehovløser(
            rapidsConnection = testRapid,
            henvendelseMediator =
                mockk<HenvendelseMediator>(relaxed = true).also {
                    every { it.taImotHenvendelse(capture(slot)) } returns HåndterHenvendelseResultat.UhåndtertHenvendelse
                },
        )
        testRapid.sendTestMessage(
            key = testIdent,
            message =
                håndterHenvendelseBehov(
                    ident = testIdent,
                    journalpostId = journalPostId,
                    registrertTidspunkt = nå,
                    søknadId = null,
                    skjemaKode = "NAVe",
                    kategori = Kategori.KLAGE,
                ),
        )

        slot.captured.let {
            it.ident shouldBe testIdent
            it.journalpostId shouldBe journalPostId
            it.registrertTidspunkt shouldBe nå
            it.søknadId shouldBe null
            it.skjemaKode shouldBe "NAVe"
            it.kategori shouldBe Kategori.KLAGE
        }

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name" : "behov",
              "@behov" : [ "HåndterHenvendelse" ],
              "journalpostId" : "$journalPostId",
              "fødselsnummer" : "$testIdent",
              "kategori" : "KLAGE",
              "@løsning" : {
                "HåndterHenvendelse": {
                  "håndtert" : false
                  }
              },
              "@final": true
            }
            """.trimIndent()
    }

    private fun håndterHenvendelseBehov(
        ident: String,
        journalpostId: String,
        registrertTidspunkt: LocalDateTime,
        søknadId: UUID? = null,
        skjemaKode: String = "NAV 04-07.08",
        kategori: Kategori,
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
              "kategori" : "${kategori.name}",
              "skjemaKode" : "$skjemaKode",
              "registrertDato" : "$registrertTidspunkt"
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
              "kategori" : "${kategori.name}",
              "skjemaKode" : "$skjemaKode",
              "registrertDato" : "$registrertTidspunkt"
            }
            """.trimIndent()
    }
}
