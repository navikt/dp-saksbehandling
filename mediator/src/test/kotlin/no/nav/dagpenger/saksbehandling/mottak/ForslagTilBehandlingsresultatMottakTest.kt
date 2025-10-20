package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import org.junit.jupiter.api.Test
import kotlin.also

class ForslagTilBehandlingsresultatMottakTest {
    private val testRapid = TestRapid()
    private val behandlingId = UUIDv7.ny()
    private val søknadId = UUIDv7.ny()
    private val ident = "123456678912"

    @Test
    fun `Skal håndtere relevante pakker`() {
        val slots = mutableListOf<ForslagTilVedtakHendelse>()
        val oppgaveMediator =
            mockk<OppgaveMediator>().also {
                every { it.opprettEllerOppdaterOppgave(capture(slots)) }
            }
        ForslagTilBehandlingsresultatMottak(testRapid, oppgaveMediator)

        listOf("Søknad", "Meldekort", "Manuell").forEachIndexed { index, behandletHendelseType ->
            testRapid.sendTestMessage(testMessage(behandletHendelseType = behandletHendelseType))
            slots[index].let { hendelse ->
                hendelse.ident shouldBe ident
                hendelse.behandletHendelseId shouldBe søknadId.toString()
                hendelse.behandlingId shouldBe behandlingId
                hendelse.behandletHendelseType shouldBe behandletHendelseType
            }
        }

        slots.single { it.behandletHendelseType == "Søknad" }.emneknagger shouldBe setOf(AVSLAG.visningsnavn)
        slots.single { it.behandletHendelseType == "Meldekort" }.emneknagger shouldBe emptySet()
        slots.single { it.behandletHendelseType == "Manuell" }.emneknagger shouldBe emptySet()
    }

    private fun testMessage(behandletHendelseType: String): String {
        //language=JSON
        return """
            {
              "@event_name": "forslag_til_behandlingsresultat",
              "ident": "$ident",
              "behandlingId": "$behandlingId",
              "behandletHendelse": {
                "id": "$søknadId",
                "type": "$behandletHendelseType"
              },
              "opplysninger": [],
              "rettighetsperioder": []
            }
            """.trimIndent()
    }
}
