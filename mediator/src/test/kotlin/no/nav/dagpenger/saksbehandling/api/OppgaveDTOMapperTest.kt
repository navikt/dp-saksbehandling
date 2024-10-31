package no.nav.dagpenger.saksbehandling.api

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
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OppgaveDTOMapperTest {
    private val oppgaveId = UUIDv7.ny()

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
