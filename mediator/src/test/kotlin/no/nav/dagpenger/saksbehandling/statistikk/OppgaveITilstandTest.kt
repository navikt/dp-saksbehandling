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
    fun `Skal konkatinere alle rettigheter i en streng med understrek mellom`() {
        oppgaveITilstand.sakYtelse() shouldBe null
        oppgaveITilstand.asMap()["sakYtelse"] shouldBe null
        oppgaveITilstand
            .copy(
                emneknagger =
                    setOf(
                        Emneknagg.Regelknagg.RETTIGHET_ORDINÆR.visningsnavn,
                        Emneknagg.Regelknagg.RETTIGHET_PERMITTERT.visningsnavn,
                        Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK.visningsnavn,
                        Emneknagg.Regelknagg.RETTIGHET_KONKURS.visningsnavn,
                        Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT.visningsnavn,
                        Emneknagg.Regelknagg.RETTIGHET_UTDANNING_OPPLÆRING.visningsnavn,
                        Emneknagg.Regelknagg.RETTIGHET_EKSPORT.visningsnavn,
                        Emneknagg.Regelknagg.RETTIGHET_ETABLERING.visningsnavn,
                    ),
            ).also {
                it.sakYtelse() shouldBe "ORDINÆR_PERMITTERT_PERMITTERTFISK_LØNNSGARANTI_VERNEPLIKT_UTDANNING_EKSPORT_ETABLERING"
                it.asMap()["sakYtelse"] shouldBe "ORDINÆR_PERMITTERT_PERMITTERTFISK_LØNNSGARANTI_VERNEPLIKT_UTDANNING_EKSPORT_ETABLERING"
            }
    }
}
