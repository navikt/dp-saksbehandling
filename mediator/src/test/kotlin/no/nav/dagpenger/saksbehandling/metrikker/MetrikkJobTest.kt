package no.nav.dagpenger.saksbehandling.metrikker

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerLåsAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerOpplåsingAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilKontroll
import no.nav.dagpenger.saksbehandling.Oppgave.PåVent
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.lagUtsending
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import org.junit.jupiter.api.Test

class MetrikkJobTest {
    @Test
    fun `Hent riktig distribusjon av oppgavetilstand`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val repo = PostgresOppgaveRepository(ds, personRepository)
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
            val personRepository = PostgresPersonRepository(ds)
            val oppgaveRepository = PostgresOppgaveRepository(ds, personRepository)
            val utsendingRepository = PostgresUtsendingRepository(ds)

            listOf(
                Utsending.VenterPåVedtak,
                Utsending.VenterPåVedtak,
                Utsending.AvventerArkiverbarVersjonAvBrev,
                Utsending.AvventerArkiverbarVersjonAvBrev,
                Utsending.AvventerJournalføring,
                Utsending.Distribuert,
                Utsending.Distribuert,
                Utsending.Distribuert,
            ).forEach {
                val oppgave = lagOppgave()
                oppgaveRepository.lagre(oppgave)
                utsendingRepository.lagre(lagUtsending(tilstand = it, oppgave.oppgaveId))
            }

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
}
