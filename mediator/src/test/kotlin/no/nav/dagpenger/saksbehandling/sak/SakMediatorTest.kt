package no.nav.dagpenger.saksbehandling.sak

import PersonMediator
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
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
import no.nav.dagpenger.saksbehandling.Behandling
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
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource

class SakMediatorTest {
    private val testIdent = "12345678901"
    private val behandlingskjedeId = UUIDv7.ny()
    private val søknadIdNyRett = UUIDv7.ny()
    private val søknadIdGjenopptak = UUIDv7.ny()
    private val endaEnSøknadId = UUIDv7.ny()
    private val meldekortId = "123L"
    private val manuellId = UUIDv7.ny()
    private val behandlingIdSøknadNyRett = UUIDv7.ny()
    private val behandlingIdSøknadGjenopptak = UUIDv7.ny()
    private val behandlingIdEndaEnSøknad = UUIDv7.ny()
    private val behandlingIdMeldekort = UUIDv7.ny()
    private val behandlingIdManuell = UUIDv7.ny()
    private val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.8")
    private val opprettetLittSenere = LocalDateTime.parse("2025-08-21T11:41:52.8")
    private val opprettetNå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    private val søknadsbehandlingOpprettetHendelseNyRett =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadIdNyRett,
            behandlingId = behandlingIdSøknadNyRett,
            ident = testIdent,
            opprettet = opprettet,
            behandlingskjedeId = behandlingskjedeId,
        )
    private val endaEnSøknadsbehandlingOpprettetHendelseNyRett =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = endaEnSøknadId,
            behandlingId = behandlingIdEndaEnSøknad,
            ident = testIdent,
            opprettet = opprettetLittSenere,
            behandlingskjedeId = UUIDv7.ny(),
        )
    private val søknadsbehandlingOpprettetHendelseGjenopptak =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadIdGjenopptak,
            behandlingId = behandlingIdSøknadGjenopptak,
            ident = testIdent,
            opprettet = opprettet,
            basertPåBehandling = søknadsbehandlingOpprettetHendelseNyRett.behandlingId,
            behandlingskjedeId = behandlingskjedeId,
        )
    private val meldekortbehandlingOpprettetHendelse =
        MeldekortbehandlingOpprettetHendelse(
            meldekortId = meldekortId,
            behandlingId = behandlingIdMeldekort,
            ident = testIdent,
            opprettet = opprettet,
            basertPåBehandling = behandlingIdSøknadNyRett,
            behandlingskjedeId = behandlingskjedeId,
        )

    private val manuellBehandlingOpprettetHendelse =
        ManuellBehandlingOpprettetHendelse(
            manuellId = manuellId,
            behandlingId = behandlingIdManuell,
            ident = testIdent,
            opprettet = opprettet,
            basertPåBehandling = behandlingIdSøknadNyRett,
            behandlingskjedeId = behandlingskjedeId,
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
                    sak.søknadId shouldBe søknadIdNyRett
                    sak.opprettet shouldBe opprettet
                    sak.behandlinger().single().behandlingId shouldBe behandlingIdSøknadNyRett
                    sak.behandlinger().single().utløstAv shouldBe UtløstAvType.SØKNAD
                }
            }
        }
    }

    @Test
    fun `Skal feile ved opprette sak dersom søknadsbehandlingOpprettetHendelse mangler behandlingskjedeId`() {
        shouldThrow<IllegalArgumentException> {
            SakMediator(
                sakRepository = mockk(),
                personMediator = mockk(),
            ).opprettSak(
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadIdNyRett,
                    behandlingId = behandlingIdSøknadNyRett,
                    ident = testIdent,
                    opprettet = opprettet,
                ),
            )
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
                    sak.søknadId shouldBe søknadIdNyRett
                    sak.opprettet shouldBe opprettet
                    sak.behandlinger().single().behandlingId shouldBe behandlingIdSøknadNyRett
                }
            }
            sakMediator.knyttTilSak(søknadsbehandlingOpprettetHendelseGjenopptak)
            sakMediator.hentSakHistorikk(søknadsbehandlingOpprettetHendelseNyRett.ident).let {
                it.person.ident shouldBe testIdent
                it.saker().single().let { sak ->
                    sak.søknadId shouldBe søknadIdNyRett
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
                    behandletHendelseType = UtløstAvType.SØKNAD,
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
                    behandletHendelseType = UtløstAvType.SØKNAD,
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

    private fun DataSource.finnArenaSakId(sakId: UUID): String? =
        sessionOf(this).use { session ->
            session.run(
                queryOf(
                    statement = """ SELECT arena_sak_id  FROM sak_v2 WHERE  id = :sak_id """,
                    paramMap = mapOf("sak_id" to sakId),
                ).map { row ->
                    row.stringOrNull("arena_sak_id")
                }.asSingle,
            )
        }

    @Test
    fun `Skal merke sak som DP-sak samt hente sakId for siste DP-sak og sakId for søknad hvis DP-sak`() {
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

            sakMediator.finnSisteSakId(ident = testIdent) shouldBe null

            ds.finnMerkeForDpSak(sakId = sak.sakId) shouldBe false
            sakMediator.merkSakenSomDpSak(
                VedtakFattetHendelse(
                    behandlingId = behandlingIdSøknadNyRett,
                    behandletHendelseId = "id",
                    behandletHendelseType = UtløstAvType.SØKNAD,
                    ident = testIdent,
                    sak =
                        UtsendingSak(
                            id = sak.sakId.toString(),
                            kontekst = "Dagpenger",
                        ),
                    automatiskBehandlet = false,
                ),
            )
            ds.finnMerkeForDpSak(sakId = sak.sakId) shouldBe true

            sakMediator.finnSisteSakId(ident = testIdent) shouldBe sak.sakId
            sakMediator.finnSakIdForSøknad(søknadId = sak.søknadId, ident = testIdent) shouldBe sak.sakId
        }
    }

    private fun DataSource.finnMerkeForDpSak(sakId: UUID): Boolean =
        sessionOf(this).use { session ->
            session.run(
                queryOf(
                    statement = """ SELECT er_dp_sak FROM sak_v2 WHERE id = :sak_id """,
                    paramMap = mapOf("sak_id" to sakId),
                ).map { row ->
                    row.boolean("er_dp_sak")
                }.asSingle,
            ) as Boolean
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
            packet["ident"].asText() shouldBe testIdent
            packet["årsak"].asText() shouldBe "Skjermet eller adressebeskyttet person"
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
            packet["ident"].asText() shouldBe testIdent
            packet["årsak"].asText() shouldBe "Skjermet eller adressebeskyttet person"
        }
    }

    @Test
    fun `Skal knytte ettersending til samme sak som søknad`() {
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
            sakMediator.merkSakenSomDpSak(
                VedtakFattetHendelse(
                    behandlingId = behandlingIdSøknadNyRett,
                    behandletHendelseId = søknadIdNyRett.toString(),
                    behandletHendelseType = UtløstAvType.SØKNAD,
                    ident = testIdent,
                    sak = null,
                ),
            )
            sakMediator.hentSakHistorikk(søknadsbehandlingOpprettetHendelseNyRett.ident).let {
                it.person.ident shouldBe testIdent
                it.saker().single().let { sak ->
                    sak.søknadId shouldBe søknadIdNyRett
                    sak.opprettet shouldBe opprettet
                    sak.behandlinger().single().behandlingId shouldBe behandlingIdSøknadNyRett
                }
            }

            sakMediator.opprettSak(endaEnSøknadsbehandlingOpprettetHendelseNyRett)
            sakMediator.merkSakenSomDpSak(
                VedtakFattetHendelse(
                    behandlingId = behandlingIdEndaEnSøknad,
                    behandletHendelseId = endaEnSøknadId.toString(),
                    behandletHendelseType = UtløstAvType.SØKNAD,
                    ident = testIdent,
                    sak = null,
                ),
            )
            val sakHistorikkFørEttersending =
                sakMediator.hentSakHistorikk(endaEnSøknadsbehandlingOpprettetHendelseNyRett.ident)
            sakHistorikkFørEttersending.saker().size shouldBe 2

            val innsendingMottattHendelse =
                InnsendingMottattHendelse(
                    ident = testIdent,
                    journalpostId = "journalpost",
                    registrertTidspunkt = opprettetNå,
                    søknadId = søknadIdNyRett,
                    skjemaKode = "skjema",
                    kategori = Kategori.ETTERSENDING,
                )
            val innsendingBehandling =
                Behandling(
                    behandlingId = UUIDv7.ny(),
                    opprettet = opprettetNå,
                    hendelse = innsendingMottattHendelse,
                    utløstAv = UtløstAvType.INNSENDING,
                )

            sakMediator.knyttEttersendingTilSammeSakSomSøknad(
                behandling = innsendingBehandling,
                hendelse = innsendingMottattHendelse,
            )

            val sakHistorikk = sakMediator.hentSakHistorikk(testIdent)

            sakHistorikk.finnSak { it.søknadId == søknadIdNyRett }?.let { sak ->
                sak.behandlinger().size shouldBe 2
                sak.behandlinger().first() shouldBe innsendingBehandling
            } ?: fail("Sak med søknadId $søknadIdNyRett ikke funnet")
        }
    }

    @Test
    fun `Skal knytte innsending til en gitt sak`() {
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
                    sak.søknadId shouldBe søknadIdNyRett
                    sak.opprettet shouldBe opprettet
                    sak.behandlinger().single().behandlingId shouldBe behandlingIdSøknadNyRett
                }
            }

            val sak = sakMediator.opprettSak(endaEnSøknadsbehandlingOpprettetHendelseNyRett)
            sakMediator.hentSakHistorikk(endaEnSøknadsbehandlingOpprettetHendelseNyRett.ident).saker().size shouldBe 2

            val hendelse =
                InnsendingMottattHendelse(
                    ident = testIdent,
                    journalpostId = "journalpost",
                    registrertTidspunkt = opprettetNå,
                    søknadId = null,
                    skjemaKode = "skjema",
                    kategori = Kategori.GENERELL,
                )
            val behandling =
                Behandling(
                    behandlingId = UUIDv7.ny(),
                    opprettet = opprettetNå,
                    hendelse = hendelse,
                    utløstAv = UtløstAvType.INNSENDING,
                )

            sakMediator.knyttBehandlingTilSak(
                behandling = behandling,
                hendelse = hendelse,
                sakId = sak.sakId,
            )

            val sakHistorikk = sakMediator.hentSakHistorikk(endaEnSøknadsbehandlingOpprettetHendelseNyRett.ident)
            sakHistorikk.finnSak { it.sakId == sak.sakId }?.let { sak ->
                sak.behandlinger().size shouldBe 2
                sak.behandlinger().first() shouldBe behandling
            } ?: fail("Sak med id ${sak.sakId} ikke funnet")
        }
    }
}
