package no.nav.dagpenger.saksbehandling.utsending

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.TestHelper.lagBehandling
import no.nav.dagpenger.saksbehandling.TestHelper.lagPerson
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Avbrutt
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerArkiverbarVersjonAvBrev
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerDistribuering
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerJournalføring
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.VenterPåVedtak
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import javax.sql.DataSource

class UtsendingAlarmRepositoryTest {
    @Test
    fun `Skal kunne hente utsendinger som ikke er ferdig distribuert`() {
        val nå = LocalDateTime.now()
        val for59MinutterSiden = nå.minusMinutes(59)
        val for1TimeOg1MinuttSiden = nå.minusMinutes(61)
        val fir24TimerSiden = nå.minusHours(24)
        val person = lagPerson()
        val behandling1 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = HendelseBehandler.DpBehandling.Søknad)
        val behandling2 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = HendelseBehandler.DpBehandling.Søknad)
        val behandling3 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = HendelseBehandler.Intern.Klage)
        val behandling4 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = HendelseBehandler.Intern.Klage)
        val behandling5 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = HendelseBehandler.DpBehandling.Søknad)
        val behandling6 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = HendelseBehandler.DpBehandling.Søknad)
        val behandling7 = lagBehandling(behandlingId = UUIDv7.ny(), utløstAvType = HendelseBehandler.DpBehandling.Søknad)

        DBTestHelper.withBehandlinger(
            person = person,
            behandlinger = listOf(behandling1, behandling2, behandling3, behandling4, behandling5, behandling6, behandling7),
        ) { ds ->
            val repository = UtsendingAlarmRepository(ds)
            ds.lagreUtsending(
                tilstand = VenterPåVedtak,
                tidspunkt = fir24TimerSiden,
                behandling = behandling1,
                person = person,
            )
            ds.lagreUtsending(
                tilstand = AvventerArkiverbarVersjonAvBrev,
                tidspunkt = for1TimeOg1MinuttSiden,
                behandling = behandling2,
                person = person,
            )
            ds.lagreUtsending(
                tilstand = AvventerJournalføring,
                tidspunkt = fir24TimerSiden,
                behandling = behandling3,
                person = person,
            )
            ds.lagreUtsending(
                tilstand = AvventerDistribuering,
                tidspunkt = for1TimeOg1MinuttSiden,
                behandling = behandling4,
                person = person,
            )
            // Denne skal ikke hentes fordi den er distribuert
            ds.lagreUtsending(
                tilstand = Distribuert,
                tidspunkt = fir24TimerSiden,
                behandling = behandling5,
                person = person,
            )
            // Denne skal ikke hentes fordi den er avbrutt
            ds.lagreUtsending(
                tilstand = Avbrutt,
                tidspunkt = fir24TimerSiden,
                behandling = behandling6,
                person = person,
            )
            // Denne skal ikke hentes fordi den er yngre enn 1 time
            ds.lagreUtsending(
                tilstand = VenterPåVedtak,
                tidspunkt = for59MinutterSiden,
                behandling = behandling7,
                person = person,
            )

            val ventendeUtsendinger = repository.hentVentendeUtsendinger(intervallAntallTimer = 1)
            ventendeUtsendinger.size shouldBe 4
            ventendeUtsendinger.map { it.tilstand } shouldBe
                listOf(
                    VenterPåVedtak.name,
                    AvventerArkiverbarVersjonAvBrev.name,
                    AvventerJournalføring.name,
                    AvventerDistribuering.name,
                )
        }
    }

    private fun DataSource.lagreUtsending(
        tilstand: Utsending.Tilstand.Type,
        tidspunkt: LocalDateTime = LocalDateTime.now(),
        behandling: Behandling = lagBehandling(utløstAvType = HendelseBehandler.DpBehandling.Søknad),
        person: Person = lagPerson(),
    ): Utsending {
        val utsending =
            Utsending(
                behandlingId = behandling.behandlingId,
                brev = "brev",
                ident = person.ident,
            )
        sessionOf(this).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO utsending_v1 ( 
                            id, 
                            behandling_id, 
                            tilstand,
                            registrert_tidspunkt, 
                            endret_tidspunkt
                        )
                        VALUES (
                            :id, 
                            :behandling_id, 
                            :tilstand, 
                            :tidspunkt, 
                            :tidspunkt
                        )
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "id" to utsending.id,
                            "behandling_id" to utsending.behandlingId,
                            "tilstand" to tilstand.name,
                            "tidspunkt" to tidspunkt,
                        ),
                ).asUpdate,
            )
        }
        return utsending
    }
}
