package no.nav.dagpenger.saksbehandling.metrikker

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerLåsAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerOpplåsingAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilKontroll
import no.nav.dagpenger.saksbehandling.Oppgave.PåVent
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.lagUtsending
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit.SECONDS

class MetrikkJobTest {
    @Test
    fun `Hent riktig distribusjon av oppgavetilstand`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(lagOppgave(tilstand = PåVent))
            repo.lagre(lagOppgave(tilstand = PåVent))
            repo.lagre(lagOppgave(tilstand = KlarTilBehandling))
            repo.lagre(lagOppgave(tilstand = KlarTilBehandling))
            repo.lagre(lagOppgave(tilstand = KlarTilBehandling))
            repo.lagre(lagOppgave(tilstand = KlarTilKontroll))
            repo.lagre(lagOppgave(tilstand = AvventerLåsAvBehandling))
            repo.lagre(lagOppgave(tilstand = AvventerOpplåsingAvBehandling))

            val oppgaveTilstandDistribusjon = hentOppgaveTilstandDistribusjon(ds)

            val forventetDistribusjon =
                mapOf(
                    PAA_VENT to 2,
                    KLAR_TIL_BEHANDLING to 3,
                    KLAR_TIL_KONTROLL to 1,
                    AVVENTER_LÅS_AV_BEHANDLING to 1,
                    AVVENTER_OPPLÅSING_AV_BEHANDLING to 1,
                )

            oppgaveTilstandDistribusjon.size shouldBe forventetDistribusjon.size

            forventetDistribusjon.forEach { (tilstand, forventetAntall) ->
                val faktiskAntall =
                    oppgaveTilstandDistribusjon
                        .find { it.oppgaveTilstand == tilstand.name }?.antall

                faktiskAntall shouldBe forventetAntall
            }
        }
    }

    @Test
    fun `Hent riktig distribusjon av utsendingtilstand`() {
        withMigratedDb { ds ->
            val oppgaveRepository = PostgresOppgaveRepository(ds)
            val oppgaveId = UUIDv7.ny()
            oppgaveRepository.lagre(lagOppgave(oppgaveId = oppgaveId))
            val utsendingRepository = PostgresUtsendingRepository(ds)

            utsendingRepository.lagre(lagUtsending(tilstand = Utsending.VenterPåVedtak, oppgaveId))
            utsendingRepository.lagre(lagUtsending(tilstand = Utsending.VenterPåVedtak, oppgaveId))
            utsendingRepository.lagre(lagUtsending(tilstand = Utsending.AvventerArkiverbarVersjonAvBrev, oppgaveId))
            utsendingRepository.lagre(lagUtsending(tilstand = Utsending.AvventerArkiverbarVersjonAvBrev, oppgaveId))
            utsendingRepository.lagre(lagUtsending(tilstand = Utsending.AvventerJournalføring, oppgaveId))
            utsendingRepository.lagre(lagUtsending(tilstand = Utsending.Distribuert, oppgaveId))
            utsendingRepository.lagre(lagUtsending(tilstand = Utsending.Distribuert, oppgaveId))
            utsendingRepository.lagre(lagUtsending(tilstand = Utsending.Distribuert, oppgaveId))

            val utsendingTilstandDistribusjon = hentUtsendingTilstandDistribusjon(ds)

            val forventetDistribusjon =
                mapOf(
                    Utsending.Tilstand.Type.VenterPåVedtak to 2,
                    Utsending.Tilstand.Type.AvventerArkiverbarVersjonAvBrev to 2,
                    Utsending.Tilstand.Type.AvventerJournalføring to 1,
                    Utsending.Tilstand.Type.Distribuert to 3,
                )

            utsendingTilstandDistribusjon.size shouldBe forventetDistribusjon.size

            forventetDistribusjon.forEach { (tilstand, forventetAntall) ->
                val faktiskAntall =
                    utsendingTilstandDistribusjon
                        .find { it.utsendingTilstand == tilstand.name }?.antall

                faktiskAntall shouldBe forventetAntall
            }
        }
    }

    @Test
    fun `Hent median saksbehandlingstid`() {
        withMigratedDb { ds ->
            val oppgaveRepository = PostgresOppgaveRepository(ds)
            val oppgaveId = UUIDv7.ny()

            val nå = LocalDateTime.now()
            oppgaveRepository.lagre(
                lagOppgave(
                    oppgaveId = oppgaveId,
                    tilstand = Oppgave.FerdigBehandlet,
                    tilstandslogg =
                        lagTilstandslogg(
                            tidspunktUnderBehandling = nå.minusMinutes(10),
                            tidspunktFerdigBehandlet = nå,
                        ),
                ),
            )
            oppgaveRepository.lagre(
                lagOppgave(
                    oppgaveId = oppgaveId,
                    tilstand = Oppgave.FerdigBehandlet,
                    tilstandslogg =
                        lagTilstandslogg(
                            tidspunktUnderBehandling = nå.minusMinutes(12),
                            tidspunktFerdigBehandlet = nå,
                        ),
                ),
            )
            oppgaveRepository.lagre(
                lagOppgave(
                    oppgaveId = oppgaveId,
                    tilstand = Oppgave.FerdigBehandlet,
                    tilstandslogg =
                        lagTilstandslogg(
                            tidspunktUnderBehandling = nå.minusMinutes(14),
                            tidspunktFerdigBehandlet = nå,
                        ),
                ),
            )

            val medianSaksbehandlingstid = medianSaksbehandlingstidSekunder(ds)
            medianSaksbehandlingstid shouldBe 12.minutes.toDouble(SECONDS)
        }
    }

    private fun lagTilstandslogg(
        tidspunktUnderBehandling: LocalDateTime,
        tidspunktFerdigBehandlet: LocalDateTime,
    ) = Tilstandslogg(
        tilstandsendringer =
            mutableListOf(
                Tilstandsendring(
                    tilstand = UNDER_BEHANDLING,
                    hendelse =
                        NesteOppgaveHendelse(
                            ansvarligIdent = "saksbehandler",
                            utførtAv = Saksbehandler("saksbehandler", emptySet()),
                        ),
                    tidspunkt = tidspunktUnderBehandling,
                ),
                Tilstandsendring(
                    tilstand = FERDIG_BEHANDLET,
                    hendelse =
                        NesteOppgaveHendelse(
                            ansvarligIdent = "saksbehandler",
                            utførtAv = Saksbehandler("saksbehandler", emptySet()),
                        ),
                    tidspunkt = tidspunktFerdigBehandlet,
                ),
            ),
    )
}
