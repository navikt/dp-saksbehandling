package no.nav.dagpenger.saksbehandling.sak

import PersonMediator
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresRepository
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SakMediatorTest {
    private val testIdent = "12345678901"
    private val søknadId = UUID.randomUUID()
    private val meldekortId = 123L
    private val manuellId = UUID.randomUUID()
    private val behandlingIdSøknad = UUID.randomUUID()
    private val behandlingIdMeldekort = UUID.randomUUID()
    private val behandlingIdManuell = UUID.randomUUID()
    private val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.8")
    private val søknadsbehandlingOpprettetHendelse =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = behandlingIdSøknad,
            ident = testIdent,
            opprettet = opprettet,
        )
    private val meldekortbehandlingOpprettetHendelse =
        MeldekortbehandlingOpprettetHendelse(
            meldekortId = meldekortId,
            behandlingId = behandlingIdMeldekort,
            ident = testIdent,
            opprettet = opprettet,
            basertPåBehandlinger = listOf(behandlingIdSøknad),
        )

    private val manuellBehandlingOpprettetHendelse =
        ManuellBehandlingOpprettetHendelse(
            manuellId = manuellId,
            behandlingId = behandlingIdManuell,
            ident = testIdent,
            opprettet = opprettet,
            basertPåBehandlinger = listOf(behandlingIdSøknad),
        )

    private val oppslagMock: Oppslag =
        mockk<Oppslag>(relaxed = false).also {
            coEvery { it.erSkjermetPerson(testIdent) } returns false
            coEvery { it.adressebeskyttelseGradering(testIdent) } returns AdressebeskyttelseGradering.UGRADERT
        }

    private val testRapid = TestRapid()

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `Skal opprette sak ved mottak av søknadsbehandlingOpprettetHendelse`() {
        withMigratedDb { ds ->

            val sakMediator =
                SakMediator(
                    sakRepository = PostgresRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                ).also {
                    it.setRapidsConnection(testRapid)
                }

            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelse)
            sakMediator.hentSakHistorikk(søknadsbehandlingOpprettetHendelse.ident).let {
                it.person.ident shouldBe testIdent
                it.saker().single().let { sak ->
                    sak.søknadId shouldBe søknadId
                    sak.opprettet shouldBe opprettet
                    sak.behandlinger().single().behandlingId shouldBe behandlingIdSøknad
                }
            }
        }
    }

    @Test
    fun `Skal knytte meldekortbehandling til sak ved mottak av meldekortbehandlingOpprettetHendelse`() {
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    sakRepository = PostgresRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                ).also {
                    it.setRapidsConnection(testRapid)
                }
            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelse)
            sakMediator.knyttTilSak(meldekortbehandlingOpprettetHendelse)

            sakMediator.hentSakHistorikk(testIdent).saker().single().behandlinger().let { behandlinger ->
                behandlinger.size shouldBe 2
                behandlinger.last().behandlingId shouldBe behandlingIdMeldekort
            }
        }
    }

    @Test
    fun `Skal knytte manuell behandling til sak ved mottak av manuellbehandlingOpprettetHendelse`() {
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    sakRepository = PostgresRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                ).also {
                    it.setRapidsConnection(testRapid)
                }
            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelse)
            sakMediator.knyttTilSak(manuellBehandlingOpprettetHendelse)

            sakMediator.hentSakHistorikk(testIdent).saker().single().behandlinger().let { behandlinger ->
                behandlinger.size shouldBe 2
                behandlinger.last().behandlingId shouldBe behandlingIdManuell
            }
        }
    }

    @Test
    fun `Skal sende avbrytBehandling ved adressebeskyttet person`() {
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    sakRepository = PostgresRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                ).also {
                    it.setRapidsConnection(testRapid)
                }

            coEvery { oppslagMock.adressebeskyttelseGradering(testIdent) } returns AdressebeskyttelseGradering.FORTROLIG
            coEvery { oppslagMock.erSkjermetPerson(testIdent) } returns false

            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelse)

            testRapid.inspektør.size shouldBe 1
            val packet = testRapid.inspektør.message(0)
            packet["@event_name"].asText() shouldBe "avbryt_behandling"
            packet["behandlingId"].asUUID() shouldBe behandlingIdSøknad
            packet["søknadId"].asUUID() shouldBe søknadId
            packet["ident"].asText() shouldBe testIdent
        }
    }

    @Test
    fun `Skal sende avbrytBehandling ved skjermet person`() {
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    sakRepository = PostgresRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                ).also {
                    it.setRapidsConnection(testRapid)
                }

            coEvery { oppslagMock.adressebeskyttelseGradering(testIdent) } returns AdressebeskyttelseGradering.UGRADERT
            coEvery { oppslagMock.erSkjermetPerson(testIdent) } returns true

            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelse)

            testRapid.inspektør.size shouldBe 1
            val packet = testRapid.inspektør.message(0)
            packet["@event_name"].asText() shouldBe "avbryt_behandling"
            packet["behandlingId"].asUUID() shouldBe behandlingIdSøknad
            packet["søknadId"].asUUID() shouldBe søknadId
            packet["ident"].asText() shouldBe testIdent
        }
    }
}
