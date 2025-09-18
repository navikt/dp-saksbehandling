package no.nav.dagpenger.saksbehandling.db.sak

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PostgresRepositoryTest {
    private val nå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    private val person =
        Person(
            id = UUIDv7.ny(),
            ident = "12345678901",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = UGRADERT,
        )

    private val oppgaveId = UUIDv7.ny()
    private val behandling1 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAvType = UtløstAvType.SØKNAD,
            opprettet = nå,
            oppgaveId = oppgaveId,
            hendelse = TomHendelse,
        )
    private val behandling2 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAvType = UtløstAvType.SØKNAD,
            opprettet = nå,
            hendelse = TomHendelse,
        )
    private val behandling3 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAvType = UtløstAvType.SØKNAD,
            opprettet = nå,
            hendelse = TomHendelse,
        )
    private val behandling4 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAvType = UtløstAvType.SØKNAD,
            opprettet = nå,
            hendelse = TomHendelse,
        )
    private val sak1 =
        Sak(
            søknadId = UUIDv7.ny(),
            opprettet = nå,
        ).also {
            it.leggTilBehandling(behandling1)
            it.leggTilBehandling(behandling2)
        }
    private val sak2 =
        Sak(
            søknadId = UUIDv7.ny(),
            opprettet = nå,
        ).also {
            // Emulerer out of order lesing
            it.leggTilBehandling(behandling4)
            it.leggTilBehandling(behandling3)
        }
    private val sakHistorikk =
        SakHistorikk(
            person = person,
        ).also {
            it.leggTilSak(sak1)
            it.leggTilSak(sak2)
        }

    @Test
    fun `Skal kunne lagre sakhistorikk`() {
        DBTestHelper.withPerson(person) { dataSource ->
            val sakRepository = PostgresRepository(dataSource = dataSource)
            sakRepository.lagre(sakHistorikk)
            this.leggTilOppgave(oppgaveId, behandling1.behandlingId)
            val saksHistorikkFraDB = sakRepository.hentSakHistorikk(person.ident)

            saksHistorikkFraDB shouldBe sakHistorikk

            // Sjekker at saker og behandling blir sortert kronologisk, med nyeste først
            saksHistorikkFraDB
                .saker()
                .first()
                .behandlinger()
                .first()
                .behandlingId shouldBe behandling4.behandlingId
        }
    }
}
