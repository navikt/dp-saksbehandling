package no.nav.dagpenger.saksbehandling.metrikker

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.BehandlingType.KLAGE
import no.nav.dagpenger.saksbehandling.BehandlingType.SØKNAD
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
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.lagPerson
import no.nav.dagpenger.saksbehandling.lagUtsending
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import org.junit.jupiter.api.Test

class MetrikkJobTest {
    val person = lagPerson()
    val behandling1 = lagBehandling(type = SØKNAD)
    val behandling2 = lagBehandling(type = SØKNAD)
    val behandling3 = lagBehandling(type = KLAGE)
    val behandling4 = lagBehandling(type = KLAGE)
    val behandling5 = lagBehandling(type = SØKNAD)
    val behandling6 = lagBehandling(type = SØKNAD)
    val behandling7 = lagBehandling(type = SØKNAD)
    val behandling8 = lagBehandling(type = SØKNAD)

    @Test
    fun `Hent riktig distribusjon av oppgavetilstand`() {
        DBTestHelper.withBehandlinger(
            person = person,
            behandlinger =
                listOf(
                    behandling1,
                    behandling2,
                    behandling3,
                    behandling4,
                    behandling5,
                    behandling6,
                    behandling7,
                    behandling8,
                ),
        ) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(lagOppgave(tilstand = PåVent, behandlingId = behandling1.behandlingId))
            repo.lagre(lagOppgave(tilstand = PåVent, behandlingId = behandling2.behandlingId))
            repo.lagre(lagOppgave(tilstand = KlarTilBehandling, behandlingId = behandling3.behandlingId))
            repo.lagre(lagOppgave(tilstand = KlarTilBehandling, behandlingId = behandling4.behandlingId))
            repo.lagre(lagOppgave(tilstand = KlarTilBehandling, behandlingId = behandling5.behandlingId))
            repo.lagre(lagOppgave(tilstand = KlarTilKontroll, behandlingId = behandling6.behandlingId))
            repo.lagre(lagOppgave(tilstand = AvventerLåsAvBehandling, behandlingId = behandling7.behandlingId))
            repo.lagre(lagOppgave(tilstand = AvventerOpplåsingAvBehandling, behandlingId = behandling8.behandlingId))

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
        DBTestHelper.withBehandlinger(
            person = person,
            behandlinger =
                listOf(
                    behandling1,
                    behandling2,
                    behandling3,
                    behandling4,
                    behandling5,
                    behandling6,
                    behandling7,
                    behandling8,
                ),
        ) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val utsendingRepository = PostgresUtsendingRepository(ds)
            lagOppgave(tilstand = PåVent, behandlingId = behandling1.behandlingId).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.VenterPåVedtak,
                        behandlingId = it.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = PåVent, behandlingId = behandling2.behandlingId).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.VenterPåVedtak,
                        behandlingId = it.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = KlarTilBehandling, behandlingId = behandling3.behandlingId).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.AvventerArkiverbarVersjonAvBrev,
                        behandlingId = it.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = KlarTilBehandling, behandlingId = behandling4.behandlingId).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.AvventerArkiverbarVersjonAvBrev,
                        behandlingId = it.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = KlarTilBehandling, behandlingId = behandling5.behandlingId).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.AvventerJournalføring,
                        behandlingId = it.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = KlarTilKontroll, behandlingId = behandling6.behandlingId).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.Distribuert,
                        behandlingId = it.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = AvventerLåsAvBehandling, behandlingId = behandling7.behandlingId).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.Distribuert,
                        behandlingId = it.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = AvventerOpplåsingAvBehandling, behandlingId = behandling8.behandlingId).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.Distribuert,
                        behandlingId = it.behandlingId,
                    ),
                )
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
