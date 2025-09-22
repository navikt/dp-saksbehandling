package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdKlient
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
                tilstandslogg =
                    Tilstandslogg().also {
                        it.leggTil(
                            nyTilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                            hendelse =
                                ForslagTilVedtakHendelse(
                                    behandletHendelseId = UUIDv7.ny().toString(),
                                    behandletHendelseType = "Søknad",
                                    behandlingId = UUIDv7.ny(),
                                    ident = "12345678901",
                                ),
                        )
                    },
            )

        val journalpostIdOppslag =
            RelevanteJournalpostIdOppslag(
                journalpostIdKlient =
                    mockk<JournalpostIdKlient>(relaxed = false).also {
                        coEvery { it.hentJournalpostIder(any(), any()) } returns Result.success(listOf("3", "4", "2"))
                    },
                klageRepository = mockk(),
                utsendingRepository =
                    mockk<UtsendingRepository>().also {
                        coEvery { it.finnUtsendingForBehandlingId(any()) } returns
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
        val klageBehandling = lagBehandling(type = UtløstAvType.KLAGE)
        val oppgave =
            lagOppgave(
                utløstAvType = UtløstAvType.KLAGE,
            )
        val opprettet = LocalDateTime.of(2025, 1, 1, 1, 1)
        val journalpostIdOppslag =
            RelevanteJournalpostIdOppslag(
                journalpostIdKlient = mockk(),
                klageRepository =
                    mockk<KlageRepository>().also {
                        coEvery { it.hentKlageBehandling(any()) } returns
                            KlageBehandling.rehydrer(
                                behandlingId = klageBehandling.behandlingId,
                                journalpostId = "1",
                                tilstand = KlageBehandling.Behandles,
                                behandlendeEnhet = null,
                                opprettet = opprettet,
                            )
                    },
                utsendingRepository =
                    mockk<UtsendingRepository>().also {
                        coEvery { it.finnUtsendingForBehandlingId(any()) } returns
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
