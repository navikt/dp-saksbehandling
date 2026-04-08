package no.nav.dagpenger.saksbehandling.generell

import PersonMediator
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.GenerellOppgaveData
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.generell.GenerellOppgaveDataRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import org.junit.jupiter.api.Test

class OpprettOppgaveMottakTest {
    private val testRapid = TestRapid()
    private val personMediator = mockk<PersonMediator>()
    private val oppgaveRepository = mockk<OppgaveRepository>(relaxed = true)
    private val sakRepository = mockk<SakRepository>(relaxed = true)
    private val generellOppgaveDataRepository = mockk<GenerellOppgaveDataRepository>(relaxed = true)
    private val ident = "12345678901"
    private val testPerson =
        Person(
            id = UUIDv7.ny(),
            ident = ident,
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )

    init {
        every { personMediator.finnEllerOpprettPerson(ident) } returns testPerson
        OpprettOppgaveMottak(
            rapidsConnection = testRapid,
            personMediator = personMediator,
            oppgaveRepository = oppgaveRepository,
            sakRepository = sakRepository,
            generellOppgaveDataRepository = generellOppgaveDataRepository,
        )
    }

    @Test
    fun `Skal opprette generell oppgave fra opprett_oppgave hendelse`() {
        testRapid.sendTestMessage(
            opprettOppgaveMelding(
                oppgaveType = "MeldekortKorrigering",
                tittel = "Meldekort trenger korrigering",
                beskrivelse = "Meldekortet for perioden 01.03-14.03 må gjennomgås",
            ),
        )

        val oppgaveSlot = slot<Oppgave>()
        verify(exactly = 1) { oppgaveRepository.lagre(capture(oppgaveSlot)) }

        val oppgave = oppgaveSlot.captured
        oppgave.behandling.utløstAv shouldBe UtløstAvType.GENERELL
        oppgave.person shouldBe testPerson
        oppgave.emneknagger shouldBe setOf("MeldekortKorrigering")
        oppgave.tilstand().type shouldBe Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING

        val dataSlot = slot<GenerellOppgaveData>()
        verify(exactly = 1) { generellOppgaveDataRepository.lagre(capture(dataSlot)) }

        val data = dataSlot.captured
        data.oppgaveType shouldBe "MeldekortKorrigering"
        data.tittel shouldBe "Meldekort trenger korrigering"
        data.beskrivelse shouldBe "Meldekortet for perioden 01.03-14.03 må gjennomgås"
        data.oppgaveId shouldBe oppgave.oppgaveId
    }

    @Test
    fun `Skal opprette generell oppgave med strukturert data`() {
        testRapid.sendTestMessage(
            //language=json
            """
            {
              "@event_name": "opprett_oppgave",
              "ident": "$ident",
              "oppgaveType": "PDLFlytting",
              "tittel": "Person har flyttet",
              "strukturertData": {
                "fraAdresse": "Oslo",
                "tilAdresse": "Bergen"
              }
            }
            """.trimIndent(),
        )

        val dataSlot = slot<GenerellOppgaveData>()
        verify(exactly = 1) { generellOppgaveDataRepository.lagre(capture(dataSlot)) }

        val data = dataSlot.captured
        data.oppgaveType shouldBe "PDLFlytting"
        data.strukturertData shouldNotBe null
        data.strukturertData!!["fraAdresse"].asText() shouldBe "Oslo"
    }

    @Test
    fun `Skal opprette generell oppgave uten valgfrie felter`() {
        testRapid.sendTestMessage(
            //language=json
            """
            {
              "@event_name": "opprett_oppgave",
              "ident": "$ident",
              "oppgaveType": "EnkelOppgave",
              "tittel": "En enkel oppgave"
            }
            """.trimIndent(),
        )

        val dataSlot = slot<GenerellOppgaveData>()
        verify(exactly = 1) { generellOppgaveDataRepository.lagre(capture(dataSlot)) }

        val data = dataSlot.captured
        data.beskrivelse shouldBe null
        data.strukturertData shouldBe null
    }

    @Test
    fun `Skal ignorere meldinger som ikke er opprett_oppgave`() {
        testRapid.sendTestMessage(
            //language=json
            """
            {
              "@event_name": "noe_annet",
              "ident": "$ident",
              "oppgaveType": "Test",
              "tittel": "Test"
            }
            """.trimIndent(),
        )

        verify(exactly = 0) { oppgaveRepository.lagre(any()) }
    }

    @Test
    fun `Skal lagre stub-behandling uten sak`() {
        testRapid.sendTestMessage(
            opprettOppgaveMelding(
                oppgaveType = "Test",
                tittel = "Test oppgave",
            ),
        )

        verify(exactly = 1) {
            sakRepository.lagreBehandling(
                personId = testPerson.id,
                sakId = null,
                behandling = match { it.utløstAv == UtløstAvType.GENERELL },
            )
        }
    }

    //language=json
    private fun opprettOppgaveMelding(
        oppgaveType: String,
        tittel: String,
        beskrivelse: String? = null,
    ) = """
        {
          "@event_name": "opprett_oppgave",
          "ident": "$ident",
          "oppgaveType": "$oppgaveType",
          "tittel": "$tittel"
          ${beskrivelse?.let { ""","beskrivelse": "$it"""" } ?: ""}
        }
        """.trimIndent()
}
