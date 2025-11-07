package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.innsending.HåndterInnsendingResultat
import no.nav.dagpenger.saksbehandling.innsending.InnsendingMediator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class InnsendingBehovløserTest {
    private val testIdent = "12345612345"
    private val sakId = UUIDv7.ny()
    private val søknadId = UUIDv7.ny()
    private val testRapid = TestRapid()
    private val journalpostId = "123123"
    private val nå = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `Skal parse behov riktig og svare med riktig løsning der vi eier saken`() {
        val slot = slot<InnsendingMottattHendelse>()
        InnsendingBehovløser(
            rapidsConnection = testRapid,
            innsendingMediator =
                mockk<InnsendingMediator>(relaxed = true).also {
                    every { it.taImotInnsending(capture(slot)) } returns
                        HåndterInnsendingResultat.HåndtertInnsending(sakId)
                },
        )
        testRapid.sendTestMessage(
            key = testIdent,
            message =
                håndterInnsendingBehov(
                    ident = testIdent,
                    journalpostId = journalpostId,
                    registrertTidspunkt = nå,
                    søknadId = søknadId,
                    skjemaKode = "NAVe",
                    kategori = Kategori.ETTERSENDING,
                ),
        )

        slot.captured.let {
            it.ident shouldBe testIdent
            it.journalpostId shouldBe journalpostId
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
              "@behov" : [ "HåndterInnsending" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdent",
              "kategori" : "ETTERSENDING",
              "@løsning" : {
                "HåndterInnsending": {
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
        val slot = slot<InnsendingMottattHendelse>()
        InnsendingBehovløser(
            rapidsConnection = testRapid,
            innsendingMediator =
                mockk<InnsendingMediator>(relaxed = true).also {
                    every { it.taImotInnsending(capture(slot)) } returns HåndterInnsendingResultat.UhåndtertInnsending
                },
        )
        testRapid.sendTestMessage(
            key = testIdent,
            message =
                håndterInnsendingBehov(
                    ident = testIdent,
                    journalpostId = journalpostId,
                    registrertTidspunkt = nå,
                    søknadId = null,
                    skjemaKode = "NAVe",
                    kategori = Kategori.KLAGE,
                ),
        )

        slot.captured.let {
            it.ident shouldBe testIdent
            it.journalpostId shouldBe journalpostId
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
              "@behov" : [ "HåndterInnsending" ],
              "journalpostId" : "$journalpostId",
              "fødselsnummer" : "$testIdent",
              "kategori" : "KLAGE",
              "@løsning" : {
                "HåndterInnsending": {
                  "håndtert" : false
                  }
              },
              "@final": true
            }
            """.trimIndent()
    }

    private fun håndterInnsendingBehov(
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
              "@behov" : [ "HåndterInnsending" ],
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
              "@behov" : [ "HåndterInnsending" ],
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
