package no.nav.dagpenger.saksbehandling.sak

import PersonMediator
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.KnyttTilSakResultat
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class SakMediatorTest {
    private val testIdent = "12345678901"
    private val søknadId = UUID.randomUUID()
    private val meldekortId = "123L"
    private val manuellId = UUID.randomUUID()
    private val behandlingIdSøknadNyRett = UUIDv7.ny()
    private val behandlingIdSøknadGjenopptak = UUIDv7.ny()
    private val behandlingIdMeldekort = UUIDv7.ny()
    private val behandlingIdManuell = UUIDv7.ny()
    private val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.8")
    private val søknadsbehandlingOpprettetHendelseNyRett =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = behandlingIdSøknadNyRett,
            ident = testIdent,
            opprettet = opprettet,
        )
    private val søknadsbehandlingOpprettetHendelseGjenopptak =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = behandlingIdSøknadGjenopptak,
            ident = testIdent,
            opprettet = opprettet,
            basertPåBehandling = søknadsbehandlingOpprettetHendelseNyRett.behandlingId,
        )
    private val meldekortbehandlingOpprettetHendelse =
        MeldekortbehandlingOpprettetHendelse(
            meldekortId = meldekortId,
            behandlingId = behandlingIdMeldekort,
            ident = testIdent,
            opprettet = opprettet,
            basertPåBehandling = behandlingIdSøknadNyRett,
        )

    private val manuellBehandlingOpprettetHendelse =
        ManuellBehandlingOpprettetHendelse(
            manuellId = manuellId,
            behandlingId = behandlingIdManuell,
            ident = testIdent,
            opprettet = opprettet,
            basertPåBehandling = behandlingIdSøknadNyRett,
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
                    sakRepository = PostgresSakRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                ).also {
                    it.setRapidsConnection(testRapid)
                }

            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelseNyRett)
            sakMediator.hentSakHistorikk(søknadsbehandlingOpprettetHendelseNyRett.ident).let {
                it.person.ident shouldBe testIdent
                it.saker().single().let { sak ->
                    sak.søknadId shouldBe søknadId
                    sak.opprettet shouldBe opprettet
                    sak.behandlinger().single().behandlingId shouldBe behandlingIdSøknadNyRett
                    sak.behandlinger().single().utløstAv shouldBe UtløstAvType.SØKNAD
                }
            }
        }
    }

    @Test
    fun `Skal knytte søknadsbehandling til eksisterende sak når basertPåBehandling er gitt`() {
        withMigratedDb { ds ->

            val sakMediator =
                SakMediator(
                    sakRepository = PostgresSakRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                ).also {
                    it.setRapidsConnection(testRapid)
                }

            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelseNyRett)
            sakMediator.hentSakHistorikk(søknadsbehandlingOpprettetHendelseNyRett.ident).let {
                it.person.ident shouldBe testIdent
                it.saker().single().let { sak ->
                    sak.søknadId shouldBe søknadId
                    sak.opprettet shouldBe opprettet
                    sak.behandlinger().single().behandlingId shouldBe behandlingIdSøknadNyRett
                }
            }
            sakMediator.knyttTilSak(søknadsbehandlingOpprettetHendelseGjenopptak)
            sakMediator.hentSakHistorikk(søknadsbehandlingOpprettetHendelseNyRett.ident).let {
                it.person.ident shouldBe testIdent
                it.saker().single().let { sak ->
                    sak.søknadId shouldBe søknadId
                    sak.opprettet shouldBe opprettet
                    sak.behandlinger().map { it.behandlingId } shouldContain behandlingIdSøknadNyRett
                    sak.behandlinger().map { it.behandlingId } shouldContain behandlingIdSøknadGjenopptak
                }
            }
        }
    }

    @Test
    fun `Skal knytte meldekortbehandling til sak ved mottak av meldekortbehandlingOpprettetHendelse`() {
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    sakRepository = PostgresSakRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                ).also {
                    it.setRapidsConnection(testRapid)
                }
            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelseNyRett)
            sakMediator.knyttTilSak(meldekortbehandlingOpprettetHendelse)

            sakMediator.hentSakHistorikk(testIdent).saker().single().behandlinger().let { behandlinger ->
                behandlinger.size shouldBe 2
                behandlinger.first().behandlingId shouldBe behandlingIdMeldekort
                behandlinger.first().utløstAv shouldBe UtløstAvType.MELDEKORT
            }
        }
    }

    @Test
    fun `Skal knytte manuell behandling til sak ved mottak av manuellbehandlingOpprettetHendelse`() {
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    sakRepository = PostgresSakRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                ).also {
                    it.setRapidsConnection(testRapid)
                }
            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelseNyRett)
            sakMediator.knyttTilSak(manuellBehandlingOpprettetHendelse)

            sakMediator.hentSakHistorikk(testIdent).saker().single().behandlinger().let { behandlinger ->
                behandlinger.size shouldBe 2
                behandlinger.first().behandlingId shouldBe behandlingIdManuell
                behandlinger.first().utløstAv shouldBe UtløstAvType.MANUELL
            }
        }
    }

    @Test
    fun `Skal oppdatere sak med arena sakId for vedtak fattet i Arena`() {
        val arenaSakId = "123"
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    sakRepository = PostgresSakRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                )
            val sak = sakMediator.opprettSak(søknadsbehandlingOpprettetHendelseNyRett)

            sakMediator.oppdaterSakMedArenaSakId(
                VedtakFattetHendelse(
                    behandlingId = behandlingIdSøknadNyRett,
                    behandletHendelseId = "id",
                    behandletHendelseType = "Søknad",
                    ident = testIdent,
                    sak =
                        UtsendingSak(
                            id = UUIDv7.ny().toString(),
                            kontekst = "Dagpenger",
                        ),
                    automatiskBehandlet = false,
                ),
            )
            ds.finnArenaSakId(sakId = sak.sakId) shouldBe null

            sakMediator.oppdaterSakMedArenaSakId(
                VedtakFattetHendelse(
                    behandlingId = behandlingIdSøknadNyRett,
                    behandletHendelseId = "id",
                    behandletHendelseType = "Søknad",
                    ident = testIdent,
                    sak =
                        UtsendingSak(
                            id = arenaSakId,
                            kontekst = "Arena",
                        ),
                    automatiskBehandlet = false,
                ),
            )
            ds.finnArenaSakId(sakId = sak.sakId) shouldBe arenaSakId
        }
    }

    private fun DataSource.finnArenaSakId(sakId: UUID): String? {
        return sessionOf(this).use { session ->
            session.run(
                queryOf(
                    statement = """ SELECT arena_sak_id  FROM sak_v2 WHERE  id = :sak_id """,
                    paramMap = mapOf("sak_id" to sakId),
                ).map { row ->
                    row.stringOrNull("arena_sak_id")
                }.asSingle,
            )
        }
    }

    @Test
    fun `Skal merke sak som DP-sak hvis dp-sak skal eie saken`() {
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    sakRepository = PostgresSakRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                )
            val sak = sakMediator.opprettSak(søknadsbehandlingOpprettetHendelseNyRett)

            ds.finnMerkeForDpSak(sakId = sak.sakId) shouldBe false
            sakMediator.merkSakenSomDpSak(
                VedtakFattetHendelse(
                    behandlingId = behandlingIdSøknadNyRett,
                    behandletHendelseId = "id",
                    behandletHendelseType = "Søknad",
                    ident = testIdent,
                    sak =
                        UtsendingSak(
                            id = UUIDv7.ny().toString(),
                            kontekst = "Dagpenger",
                        ),
                    automatiskBehandlet = false,
                ),
            )
            ds.finnMerkeForDpSak(sakId = sak.sakId) shouldBe true
        }
    }

    private fun DataSource.finnMerkeForDpSak(sakId: UUID): Boolean {
        return sessionOf(this).use { session ->
            session.run(
                queryOf(
                    statement = """ SELECT er_dp_sak FROM sak_v2 WHERE id = :sak_id """,
                    paramMap = mapOf("sak_id" to sakId),
                ).map { row ->
                    row.boolean("er_dp_sak")
                }.asSingle,
            ) as Boolean
        }
    }

    @Test
    fun `Skal sende avbrytBehandling ved adressebeskyttet person`() {
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    sakRepository = PostgresSakRepository(ds),
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

            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelseNyRett)

            testRapid.inspektør.size shouldBe 1
            val packet = testRapid.inspektør.message(0)
            packet["@event_name"].asText() shouldBe "avbryt_behandling"
            packet["behandlingId"].asUUID() shouldBe behandlingIdSøknadNyRett
            packet["søknadId"].asUUID() shouldBe søknadId
            packet["ident"].asText() shouldBe testIdent
        }
    }

    @Test
    fun `Skal sende ut alert dersom vi ikke får knyttet til en sak`() {
        val sakId = UUIDv7.ny()
        val mockSakHistorikk =
            mockk<SakHistorikk>().also {
                every { it.knyttTilSak(any<SøknadsbehandlingOpprettetHendelse>()) } returns
                    KnyttTilSakResultat.IkkeKnyttetTilSak(
                        sakId,
                    )
            }
        SakMediator(
            sakRepository =
                mockk<SakRepository>(relaxed = true).also {
                    every { it.hentSakHistorikk(testIdent) } returns mockSakHistorikk
                    every { it.lagre(mockSakHistorikk) } just Runs
                },
            personMediator = mockk(relaxed = true),
        ).also {
            it.setRapidsConnection(testRapid)
            it.knyttTilSak(søknadsbehandlingOpprettetHendelseNyRett)
            testRapid.inspektør.size shouldBe 1
            val packet = testRapid.inspektør.message(0)
            packet["@event_name"].asText() shouldBe "saksbehandling_alert"
            packet["alertType"].asText() shouldBe "KNYTNING_TIL_SAK_FEIL"
        }
    }

    @Test
    fun `Skal sende avbrytBehandling ved skjermet person`() {
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    sakRepository = PostgresSakRepository(ds),
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

            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelseNyRett)

            testRapid.inspektør.size shouldBe 1
            val packet = testRapid.inspektør.message(0)
            packet["@event_name"].asText() shouldBe "avbryt_behandling"
            packet["behandlingId"].asUUID() shouldBe behandlingIdSøknadNyRett
            packet["søknadId"].asUUID() shouldBe søknadId
            packet["ident"].asText() shouldBe testIdent
        }
    }
}
