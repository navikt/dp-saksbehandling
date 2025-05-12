package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RelevanteJournalpostIdOppslagTest {
    @Test
    fun `For søknadsbehandling skal vi først hente journalposter for søknad og ettersendinger sortert stigende og deretter utsending`() {
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
                klageRepository = mockk(),
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

    @Test
    fun `For klagebehandling skal vi først hente journalpost for klagen deretter utsending`() {
        val klageBehandling = lagBehandling(type = BehandlingType.KLAGE)
        val oppgave =
            lagOppgave(
                behandling = klageBehandling,
            )

        val journalpostIdOppslag =
            RelevanteJournalpostIdOppslag(
                journalpostIdClient = mockk(),
                klageRepository =
                    mockk<KlageRepository>().also {
                        coEvery { it.hentKlageBehandling(any()) } returns
                            KlageBehandling(
                                behandlingId = klageBehandling.behandlingId,
                                journalpostId = "1",
                            )
                    },
                utsendingRepository =
                    mockk<UtsendingRepository>().also {
                        coEvery { it.finnUtsendingFor(any()) } returns
                            mockk<Utsending>().also {
                                coEvery { it.journalpostId() } returns "5"
                            }
                    },
            )
        runBlocking {
            journalpostIdOppslag.hentJournalpostIder(oppgave) shouldBe listOf("1", "5")
        }
    }
}
