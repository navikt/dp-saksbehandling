package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand.Tilstandsendring
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class OppgaveITilstandTest {
    private val oppgaveITilstand =
        OppgaveITilstand(
            oppgaveId = UUID.randomUUID(),
            mottatt = LocalDateTime.now(),
            sakId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            personIdent = "",
            saksbehandlerIdent = "",
            beslutterIdent = "",
            versjon = "",
            tilstandsendring =
                Tilstandsendring(
                    sekvensnummer = 9968,
                    tilstandsendringId = UUID.randomUUID(),
                    tilstand = "graeci",
                    tidspunkt = LocalDateTime.now(),
                ),
            utløstAv = "",
            emneknagger = setOf(),
            behandlingResultat = "",
            behandlingÅrsak = "",
            fagsystem = "",
            arenaSakId = "",
            resultatBegrunnelse = "",
            relatertBehandlingId = null,
        )

    @Test
    fun sakYtelse() {
        oppgaveITilstand.sakYtelse() shouldBe null
        oppgaveITilstand.asMap()["sakYtelse"] shouldBe null

        var emneknaggerUnderTest =
            setOf(
                Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_PERMITTERT.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_KONKURS.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_ORDINÆR.visningsnavn,
            )

        oppgaveITilstand
            .copy(emneknagger = emneknaggerUnderTest)
            .also {
                it.sakYtelse() shouldBe "PERMITTERT_FISK"
                it.asMap()["sakYtelse"] shouldBe "PERMITTERT_FISK"
            }

        emneknaggerUnderTest = emneknaggerUnderTest.minus(Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK.visningsnavn)
        oppgaveITilstand
            .copy(emneknagger = emneknaggerUnderTest)
            .also {
                it.sakYtelse() shouldBe "PERMITTERT"
                it.asMap()["sakYtelse"] shouldBe "PERMITTERT"
            }

        emneknaggerUnderTest = emneknaggerUnderTest.minus(Emneknagg.Regelknagg.RETTIGHET_PERMITTERT.visningsnavn)
        oppgaveITilstand
            .copy(emneknagger = emneknaggerUnderTest)
            .also {
                it.sakYtelse() shouldBe "LØNNSGARANTI"
                it.asMap()["sakYtelse"] shouldBe "LØNNSGARANTI"
            }

        emneknaggerUnderTest = emneknaggerUnderTest.minus(Emneknagg.Regelknagg.RETTIGHET_KONKURS.visningsnavn)
        oppgaveITilstand
            .copy(emneknagger = emneknaggerUnderTest)
            .also {
                it.sakYtelse() shouldBe "VERNEPLIKT"
                it.asMap()["sakYtelse"] shouldBe "VERNEPLIKT"
            }

        emneknaggerUnderTest = emneknaggerUnderTest.minus(Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT.visningsnavn)
        oppgaveITilstand
            .copy(emneknagger = emneknaggerUnderTest)
            .also {
                it.sakYtelse() shouldBe "ORDINÆR"
                it.asMap()["sakYtelse"] shouldBe "ORDINÆR"
            }
    }
}
