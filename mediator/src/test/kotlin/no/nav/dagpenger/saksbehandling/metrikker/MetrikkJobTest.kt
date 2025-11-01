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
import no.nav.dagpenger.saksbehandling.TestHelper.lagBehandling
import no.nav.dagpenger.saksbehandling.TestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.TestHelper.lagPerson
import no.nav.dagpenger.saksbehandling.TestHelper.lagUtsending
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType.KLAGE
import no.nav.dagpenger.saksbehandling.UtløstAvType.SØKNAD
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import org.junit.jupiter.api.Test

class MetrikkJobTest {
    val person = lagPerson()
    val behandling1 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = SØKNAD)
    val behandling2 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = SØKNAD)
    val behandling3 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = KLAGE)
    val behandling4 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = KLAGE)
    val behandling5 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = SØKNAD)
    val behandling6 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = SØKNAD)
    val behandling7 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = SØKNAD)
    val behandling8 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = SØKNAD)

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
            repo.lagre(lagOppgave(oppgaveId = UUIDv7.ny(), tilstand = PåVent, behandling = behandling1))
            repo.lagre(lagOppgave(oppgaveId = UUIDv7.ny(), tilstand = PåVent, behandling = behandling2))
            repo.lagre(lagOppgave(oppgaveId = UUIDv7.ny(), tilstand = KlarTilBehandling, behandling = behandling3))
            repo.lagre(lagOppgave(oppgaveId = UUIDv7.ny(), tilstand = KlarTilBehandling, behandling = behandling4))
            repo.lagre(lagOppgave(oppgaveId = UUIDv7.ny(), tilstand = KlarTilBehandling, behandling = behandling5))
            repo.lagre(lagOppgave(oppgaveId = UUIDv7.ny(), tilstand = KlarTilKontroll, behandling = behandling6))
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = AvventerLåsAvBehandling,
                    behandling = behandling7,
                ),
            )
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = AvventerOpplåsingAvBehandling,
                    behandling = behandling8,
                ),
            )

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
            lagOppgave(tilstand = PåVent, behandling = behandling1).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.VenterPåVedtak,
                        behandlingId = it.behandling.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = PåVent, behandling = behandling2).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.VenterPåVedtak,
                        behandlingId = it.behandling.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = KlarTilBehandling, behandling = behandling3).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.AvventerArkiverbarVersjonAvBrev,
                        behandlingId = it.behandling.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = KlarTilBehandling, behandling = behandling4).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.AvventerArkiverbarVersjonAvBrev,
                        behandlingId = it.behandling.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = KlarTilBehandling, behandling = behandling5).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.AvventerJournalføring,
                        behandlingId = it.behandling.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = KlarTilKontroll, behandling = behandling6).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.Distribuert,
                        behandlingId = it.behandling.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = AvventerLåsAvBehandling, behandling = behandling7).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.Distribuert,
                        behandlingId = it.behandling.behandlingId,
                    ),
                )
            }
            lagOppgave(tilstand = AvventerOpplåsingAvBehandling, behandling = behandling8).also {
                repo.lagre(it)
                utsendingRepository.lagre(
                    lagUtsending(
                        tilstand = Utsending.Distribuert,
                        behandlingId = it.behandling.behandlingId,
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
