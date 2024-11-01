package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Notat
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OppgaveHistorikkDTOMapperTest {
    @Test
    fun `lage historikk`() {
        val oppgaveId = UUIDv7.ny()
        val sistEndretTidspunkt = LocalDateTime.of(2024, 11, 1, 9, 50)
        OppgaveHistorikkDTOMapper(
            repository =
                mockk<OppgaveRepository>(relaxed = true).also {
                    every { it.finnNotat(any()) } returns
                        Notat(
                            notatId = UUIDv7.ny(),
                            tekst = "Dette er et notat",
                            sistEndretTidspunkt = sistEndretTidspunkt,
                        )
                },
        ).let { mapper ->

            val historikk =
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

            historikk.size shouldBe 1
            objectMapper.writeValueAsString(historikk) shouldEqualJson """
                [
                    {
                        "type": "notat",
                        "tidspunkt": "2024-11-01T09:50:00",
                        "behandler": {
                            "navn": "utførtAv",
                            "rolle": "beslutter"
                        },
                        "tittel": "Notat",
                        "body": "Dette er et notat"
                    }
                ]
            """
        }
    }
}
