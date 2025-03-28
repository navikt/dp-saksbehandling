package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RelevanteJournalpostIdOppslagTest {
    @Test
    fun `Skal sortere journalposter stigende`() {
        val oppgave =
            lagOppgave(
                behandling =
                    lagBehandling(
                        hendelse =
                            SøknadsbehandlingOpprettetHendelse(
                                søknadId = UUIDv7.ny(),
                                behandlingId = UUIDv7.ny(),
                                ident = "12345678901",
                                opprettet = LocalDateTime.now(),
                            ),
                    ),
            )

        val journalpostIdOppslag =
            RelevanteJournalpostIdOppslag(
                journalpostIdClient =
                    mockk<JournalpostIdClient>().also {
                        coEvery { it.hentJournalpostIder(any(), any()) } returns Result.success(listOf("3", "4", "2"))
                    },
                utsendingRepository =
                    mockk<UtsendingRepository>().also {
                        coEvery { it.finnUtsendingFor(any()) } returns
                            mockk<Utsending>().also {
                                coEvery { it.journalpostId() } returns "1"
                            }
                    },
            )
        runBlocking {
            journalpostIdOppslag.hentJournalpostIder(oppgave) shouldBe listOf("2", "3", "4", "1")
        }
    }
}
