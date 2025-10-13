package no.nav.dagpenger.saksbehandling.db.sak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.søknadId
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PostgresSakRepositoryTest {
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
            utløstAv = UtløstAvType.SØKNAD,
            opprettet = nå.minusDays(9),
            oppgaveId = oppgaveId,
            hendelse = TomHendelse,
        )
    private val behandling2 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = UtløstAvType.SØKNAD,
            opprettet = nå.minusDays(5),
            hendelse = TomHendelse,
        )
    private val behandling3 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = UtløstAvType.SØKNAD,
            opprettet = nå.minusDays(1),
            hendelse = TomHendelse,
        )
    private val behandling4 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = UtløstAvType.SØKNAD,
            opprettet = nå.minusDays(3),
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
    fun `Skal kunne lagre sakHistorikk`() {
        DBTestHelper.withPerson(person) { dataSource ->
            val sakRepository = PostgresSakRepository(dataSource = dataSource)
            sakRepository.lagre(sakHistorikk)
            this.leggTilOppgave(oppgaveId, behandling1.behandlingId)
            val sakHistorikkFraDB = sakRepository.hentSakHistorikk(person.ident)

            sakHistorikkFraDB shouldBe sakHistorikk

            // Sjekker at saker og behandling blir sortert kronologisk, med nyeste først
            sakHistorikkFraDB
                .saker()
                .first()
                .behandlinger()
                .first()
                .behandlingId shouldBe behandling4.behandlingId
        }
    }

    @Test
    fun `Hent sakId basert på behandlingId`() {
        DBTestHelper.withSaker(saker = listOf(sak1)) { ds ->
            val sakRepository = PostgresSakRepository(ds)

            sakRepository.merkSakenSomDpSak(sak1.sakId, true)
            sakRepository.hentSakIdForBehandlingId(behandling1.behandlingId) shouldBe sak1.sakId
            sakRepository.hentDagpengerSakIdForBehandlingId(behandling1.behandlingId) shouldBe sak1.sakId

            sakRepository.merkSakenSomDpSak(sak1.sakId, false)
            sakRepository.hentSakIdForBehandlingId(behandling1.behandlingId) shouldBe sak1.sakId
            shouldThrow<DataNotFoundException> {
                sakRepository.hentDagpengerSakIdForBehandlingId(behandling1.behandlingId) shouldBe sak1.sakId
            }
        }
    }

    @Test
    fun `Henter sakId til nyeste dp-sak for en person`() {
        DBTestHelper.withSaker(saker = listOf(sak1, sak2)) { ds ->
            val sakRepository = PostgresSakRepository(ds)

            sakRepository.finnSisteSakId(ident = person.ident) shouldBe null

            sakRepository.merkSakenSomDpSak(sakId = sak1.sakId, erDpSak = true)
            sakRepository.finnSisteSakId(ident = person.ident) shouldBe sak1.sakId

            sakRepository.merkSakenSomDpSak(sakId = sak2.sakId, erDpSak = true)
            sakRepository.finnSisteSakId(ident = person.ident) shouldBe sak2.sakId
        }
    }

    @Test
    fun `Finner sakId for en søknad"`() {
        DBTestHelper.withSaker(saker = listOf(sak1, sak2)) { ds ->
            val sakRepository = PostgresSakRepository(ds)

            sakRepository.finnSakIdForSøknad(søknadId = sak1.søknadId) shouldBe null

            sakRepository.merkSakenSomDpSak(sakId = sak1.sakId, erDpSak = true)
            sakRepository.finnSakIdForSøknad(søknadId = sak1.søknadId) shouldBe sak1.sakId

            sakRepository.merkSakenSomDpSak(sakId = sak2.sakId, erDpSak = true)
            sakRepository.finnSakIdForSøknad(søknadId = sak2.søknadId) shouldBe sak2.sakId
        }
    }
}
