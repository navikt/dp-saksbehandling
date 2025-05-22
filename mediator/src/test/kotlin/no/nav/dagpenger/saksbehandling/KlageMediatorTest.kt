package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.klage.PostgresKlageRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.klage.HvemKlagerType
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HJEMLER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HVEM_KLAGER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST_OPPFYLT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER_VEDTAK
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER_VEDTAKSDATO
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGE_MOTTATT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.VURDERING_AV_KLAGEN
import no.nav.dagpenger.saksbehandling.klage.OversendtKlageinstansMottak
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KlageMediatorTest {
    private val testPersonIdent = "12345678901"
    private val testRapid = TestRapid()
    private val saksbehandler = Saksbehandler("saksbehandler", grupper = emptySet())
    private val behandlerDTO =
        BehandlerDTO(
            ident = saksbehandler.navIdent,
            fornavn = "Mikke",
            etternavn = "Mus",
            enhet =
                BehandlerDTOEnhetDTO(
                    navn = "NAV Arbeid og ytelser Disneyworld",
                    enhetNr = "440Gakk",
                    postadresse = "Disneyland",
                ),
        )
    private val oppslagMock =
        mockk<Oppslag>().also {
            coEvery { it.hentPersonMedSkjermingOgGradering(testPersonIdent) } returns
                Person(
                    id = UUIDv7.ny(),
                    ident = testPersonIdent,
                    skjermesSomEgneAnsatte = false,
                    adressebeskyttelseGradering = UGRADERT,
                )
        }

    private val saksbehandlerOppslagMock =
        mockk<SaksbehandlerOppslag>().also {
            coEvery { it.hentSaksbehandler(navIdent = any()) } returns behandlerDTO
        }

    @Test
    fun `Livssyklus til en klage som ferdigstilles med opprettholdelse`() {
        val fagsakId = UUIDv7.ny()
        val utsendingMediator = mockk<UtsendingMediator>(relaxed = true)
        withMigratedDb { datasource ->
            val klageRepository = PostgresKlageRepository(datasource)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = PostgresPersonRepository(datasource),
                    oppgaveRepository = PostgresOppgaveRepository(datasource),
                    behandlingKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                    oppslag = oppslagMock,
                    meldingOmVedtakKlient = mockk(),
                )
            val klageMediator =
                KlageMediator(
                    klageRepository = klageRepository,
                    oppgaveMediator = oppgaveMediator,
                    utsendingMediator = utsendingMediator,
                    saksbehandlerOppslag = saksbehandlerOppslagMock,
                ).also {
                    it.setRapidsConnection(testRapid)
                }
            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                ).behandling.behandlingId

            klageMediator.hentKlageBehandling(behandlingId, saksbehandler).tilstand().type shouldBe BEHANDLES

            val oppgave = oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)

            oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING

            oppgaveMediator.tildelOppgave(
                settOppgaveAnsvarHendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
            )

            klageMediator.registrerKlageBehandlingOpplysninger(behandlingId, saksbehandler)

            shouldThrow<IllegalStateException> {
                klageMediator.ferdigstill(
                    KlageFerdigbehandletHendelse(
                        behandlingId = behandlingId,
                        utførtAv = saksbehandler,
                    ),
                )
            }

            klageMediator.registrerUtfallOpprettholdelseOpplysninger(behandlingId, saksbehandler)
            klageMediator.ferdigstill(
                KlageFerdigbehandletHendelse(
                    behandlingId = behandlingId,
                    utførtAv = saksbehandler,
                ),
            )
            val klageBehandling =
                klageMediator.hentKlageBehandling(behandlingId = behandlingId, saksbehandler = saksbehandler)
            klageBehandling.tilstand().type shouldBe OVERSEND_KLAGEINSTANS
            klageBehandling.behandlendeEnhet() shouldBe "440Gakk"
            testRapid.inspektør.size shouldBe 1

            OversendtKlageinstansMottak(
                rapidsConnection = testRapid,
                klageMediator = klageMediator,
            )

            val melding =
                """
                {
                  "@event_name" : "behov",
                  "@behov" : [ "OversendelseKlageinstans" ],
                  "behandlingId" : "$behandlingId",
                  "ident" : "${oppgave.behandling.person.ident}",
                  "fagsakId" : "$fagsakId",
                  "behandlendeEnhet": "${klageBehandling.behandlendeEnhet()}",
                  "hjemler": ${klageBehandling.hjemler().map { "\"$it\"" }},
                  "@løsning": {
                    "OversendelseKlageinstans": "OK"
                  }
                }
                """.trimIndent()

            testRapid.sendTestMessage(
                key = oppgave.behandling.person.ident,
                message = melding,
            )

            klageMediator.hentKlageBehandling(behandlingId = behandlingId, saksbehandler = saksbehandler)
                .tilstand().type shouldBe FERDIGSTILT

            oppgaveMediator.hentOppgaveFor(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand().type shouldBe FERDIG_BEHANDLET

            verify(exactly = 1) {
                utsendingMediator.opprettUtsending(
                    oppgaveId = oppgave.oppgaveId,
                    brev = "<html><h1>Dette må vi gjøre noe med</h1></html>",
                    ident = oppgave.behandling.person.ident,
                )
            }
            verify(exactly = 1) {
                utsendingMediator.mottaStartUtsending(any())
            }
        }
    }

    @Test
    fun `Livssyklus til en manuell klage som ferdigstilles med opprettholdelse`() {
        val fagsakId = UUIDv7.ny()
        val utsendingMediator = mockk<UtsendingMediator>(relaxed = true)
        withMigratedDb { datasource ->
            val klageRepository = PostgresKlageRepository(datasource)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = PostgresPersonRepository(datasource),
                    oppgaveRepository = PostgresOppgaveRepository(datasource),
                    behandlingKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                    oppslag = oppslagMock,
                    meldingOmVedtakKlient = mockk(),
                )
            val klageMediator =
                KlageMediator(
                    klageRepository = klageRepository,
                    oppgaveMediator = oppgaveMediator,
                    utsendingMediator = utsendingMediator,
                    saksbehandlerOppslag = saksbehandlerOppslagMock,
                ).also {
                    it.setRapidsConnection(testRapid)
                }
            val behandlingId =
                klageMediator.opprettManuellKlage(
                    ManuellKlageMottattHendelse(
                        ident = testPersonIdent,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                        utførtAv = saksbehandler,
                    ),
                ).behandling.behandlingId

            klageMediator.hentKlageBehandling(behandlingId, saksbehandler).tilstand().type shouldBe BEHANDLES

            val oppgave = oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)

            oppgave.tilstand().type shouldBe UNDER_BEHANDLING
            oppgave.behandlerIdent shouldBe saksbehandler.navIdent

            klageMediator.registrerKlageBehandlingOpplysninger(behandlingId, saksbehandler)

            shouldThrow<IllegalStateException> {
                klageMediator.ferdigstill(
                    KlageFerdigbehandletHendelse(
                        behandlingId = behandlingId,
                        utførtAv = saksbehandler,
                    ),
                )
            }

            klageMediator.registrerUtfallOpprettholdelseOpplysninger(behandlingId, saksbehandler)
            klageMediator.ferdigstill(
                KlageFerdigbehandletHendelse(
                    behandlingId = behandlingId,
                    utførtAv = saksbehandler,
                ),
            )
            val klageBehandling =
                klageMediator.hentKlageBehandling(behandlingId = behandlingId, saksbehandler = saksbehandler)
            klageBehandling.tilstand().type shouldBe OVERSEND_KLAGEINSTANS
            klageBehandling.behandlendeEnhet() shouldBe "440Gakk"
            testRapid.inspektør.size shouldBe 1

            OversendtKlageinstansMottak(
                rapidsConnection = testRapid,
                klageMediator = klageMediator,
            )

            val melding =
                """
                {
                  "@event_name" : "behov",
                  "@behov" : [ "OversendelseKlageinstans" ],
                  "behandlingId" : "$behandlingId",
                  "ident" : "${oppgave.behandling.person.ident}",
                  "fagsakId" : "$fagsakId",
                  "behandlendeEnhet": "${klageBehandling.behandlendeEnhet()}",
                  "hjemler": ${klageBehandling.hjemler().map { "\"$it\"" }},
                  "@løsning": {
                    "OversendelseKlageinstans": "OK"
                  }
                }
                """.trimIndent()

            testRapid.sendTestMessage(
                key = oppgave.behandling.person.ident,
                message = melding,
            )

            klageMediator.hentKlageBehandling(behandlingId = behandlingId, saksbehandler = saksbehandler)
                .tilstand().type shouldBe FERDIGSTILT

            oppgaveMediator.hentOppgaveFor(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand().type shouldBe FERDIG_BEHANDLET

            verify(exactly = 1) {
                utsendingMediator.opprettUtsending(
                    oppgaveId = oppgave.oppgaveId,
                    brev = "<html><h1>Dette må vi gjøre noe med</h1></html>",
                    ident = oppgave.behandling.person.ident,
                )
            }
            verify(exactly = 1) {
                utsendingMediator.mottaStartUtsending(any())
            }
        }
    }

    @Test
    fun `Livssyklus til en klage som ferdigstilles med avvisning`() {
        val fagsakId = UUIDv7.ny()
        val utsendingMediator = mockk<UtsendingMediator>(relaxed = true)
        withMigratedDb { datasource ->
            val klageRepository = PostgresKlageRepository(datasource)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = PostgresPersonRepository(datasource),
                    oppgaveRepository = PostgresOppgaveRepository(datasource),
                    behandlingKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                    oppslag = oppslagMock,
                    meldingOmVedtakKlient = mockk(),
                )
            val klageMediator =
                KlageMediator(
                    klageRepository = klageRepository,
                    oppgaveMediator = oppgaveMediator,
                    utsendingMediator = utsendingMediator,
                    saksbehandlerOppslag = saksbehandlerOppslagMock,
                ).also {
                    it.setRapidsConnection(testRapid)
                }
            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                ).behandling.behandlingId

            klageMediator.hentKlageBehandling(behandlingId, saksbehandler).tilstand().type shouldBe BEHANDLES

            val oppgave = oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)

            oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING

            oppgaveMediator.tildelOppgave(
                settOppgaveAnsvarHendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
            )

            klageMediator.registrerKlageBehandlingOpplysninger(behandlingId, saksbehandler)

            shouldThrow<IllegalStateException> {
                klageMediator.ferdigstill(
                    KlageFerdigbehandletHendelse(
                        behandlingId = behandlingId,
                        utførtAv = saksbehandler,
                    ),
                )
            }

            klageMediator.registrerOpplysningerMedUtfall(behandlingId, saksbehandler, UtfallType.AVVIST)
            klageMediator.ferdigstill(
                KlageFerdigbehandletHendelse(
                    behandlingId = behandlingId,
                    utførtAv = saksbehandler,
                ),
            )
            val klageBehandling =
                klageMediator.hentKlageBehandling(behandlingId = behandlingId, saksbehandler = saksbehandler)
            klageBehandling.tilstand().type shouldBe FERDIGSTILT
            klageBehandling.utfall() shouldBe UtfallType.AVVIST
            klageBehandling.behandlendeEnhet() shouldBe "440Gakk"
            testRapid.inspektør.size shouldBe 0
        }
    }

    @Test
    fun `Livssyklus til en klage som avbrytes`() {
        val utsendingMediator = mockk<UtsendingMediator>(relaxed = true)
        withMigratedDb { datasource ->
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = PostgresPersonRepository(datasource),
                    oppgaveRepository = PostgresOppgaveRepository(datasource),
                    behandlingKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                    oppslag = oppslagMock,
                    meldingOmVedtakKlient = mockk(),
                )
            val klageMediator =
                KlageMediator(
                    klageRepository = PostgresKlageRepository(datasource),
                    oppgaveMediator = oppgaveMediator,
                    utsendingMediator = utsendingMediator,
                    saksbehandlerOppslag = saksbehandlerOppslagMock,
                )
            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                ).behandling.behandlingId

            klageMediator.hentKlageBehandling(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand().type shouldBe BEHANDLES

            val oppgave = oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)

            oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING

            oppgaveMediator.tildelOppgave(
                settOppgaveAnsvarHendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
            )

            klageMediator.avbrytKlage(
                hendelse =
                    AvbruttHendelse(
                        behandlingId = behandlingId,
                        utførtAv = saksbehandler,
                    ),
            )

            klageMediator.hentKlageBehandling(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand().type shouldBe AVBRUTT

            oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)
                .tilstand().type shouldBe FERDIG_BEHANDLET
        }
    }

    @Test
    fun `Kan ikke ferdigstille en klage med medhold`() {
        val utsendingMediator = mockk<UtsendingMediator>(relaxed = true)
        withMigratedDb { datasource ->
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = PostgresPersonRepository(datasource),
                    oppgaveRepository = PostgresOppgaveRepository(datasource),
                    behandlingKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                    oppslag = oppslagMock,
                    meldingOmVedtakKlient = mockk(),
                )
            val klageMediator =
                KlageMediator(
                    klageRepository = PostgresKlageRepository(datasource),
                    oppgaveMediator = oppgaveMediator,
                    utsendingMediator = utsendingMediator,
                    saksbehandlerOppslag = saksbehandlerOppslagMock,
                )
            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                ).behandling.behandlingId

            klageMediator.hentKlageBehandling(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand().type shouldBe BEHANDLES

            val oppgave = oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)

            oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING

            oppgaveMediator.tildelOppgave(
                settOppgaveAnsvarHendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
            )
            klageMediator.registrerKlageBehandlingOpplysninger(behandlingId, saksbehandler)

            klageMediator.registrerOpplysningerMedUtfall(behandlingId, saksbehandler, UtfallType.MEDHOLD)
            shouldThrow<IllegalStateException> {
                klageMediator.ferdigstill(
                    KlageFerdigbehandletHendelse(
                        behandlingId = behandlingId,
                        utførtAv = saksbehandler,
                    ),
                )
            }

            oppgaveMediator.hentOppgaveFor(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand().type shouldBe UNDER_BEHANDLING
        }
    }

    @Test
    fun `Kan ikke ferdigstille en klage med delvis medhold`() {
        val utsendingMediator = mockk<UtsendingMediator>(relaxed = true)
        withMigratedDb { datasource ->
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = PostgresPersonRepository(datasource),
                    oppgaveRepository = PostgresOppgaveRepository(datasource),
                    behandlingKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                    oppslag = oppslagMock,
                    meldingOmVedtakKlient = mockk(),
                )
            val klageMediator =
                KlageMediator(
                    klageRepository = PostgresKlageRepository(datasource),
                    oppgaveMediator = oppgaveMediator,
                    utsendingMediator = utsendingMediator,
                    saksbehandlerOppslag = saksbehandlerOppslagMock,
                )
            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                ).behandling.behandlingId

            klageMediator.hentKlageBehandling(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand().type shouldBe BEHANDLES

            val oppgave = oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)

            oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING

            oppgaveMediator.tildelOppgave(
                settOppgaveAnsvarHendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
            )
            klageMediator.registrerKlageBehandlingOpplysninger(behandlingId, saksbehandler)

            klageMediator.registrerOpplysningerMedUtfall(behandlingId, saksbehandler, UtfallType.DELVIS_MEDHOLD)
            shouldThrow<IllegalStateException> {
                klageMediator.ferdigstill(
                    KlageFerdigbehandletHendelse(
                        behandlingId = behandlingId,
                        utførtAv = saksbehandler,
                    ),
                )
            }

            oppgaveMediator.hentOppgaveFor(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand().type shouldBe UNDER_BEHANDLING
        }
    }

    private fun KlageMediator.registrerKlageBehandlingOpplysninger(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ) {
        fun oppdaterOpplysning(
            opplysningId: UUID,
            svar: Verdi,
        ) {
            return oppdaterKlageOpplysning(
                behandlingId = behandlingId,
                opplysningId = opplysningId,
                verdi = svar,
                saksbehandler = saksbehandler,
            )
        }

        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == KLAGEN_GJELDER_VEDTAK }.opplysningId,
            svar = Verdi.TekstVerdi("Vedtak 1"),
        )

        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == KLAGEN_GJELDER_VEDTAKSDATO }.opplysningId,
            svar = Verdi.Dato(LocalDate.MIN),
        )

        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == KLAGEFRIST }.opplysningId,
            svar = Verdi.Dato(LocalDate.MIN),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == KLAGE_MOTTATT }.opplysningId,
            svar = Verdi.Dato(LocalDate.MIN),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == KLAGEFRIST_OPPFYLT }.opplysningId,
            svar = Verdi.Boolsk(true),
        )
        this.hentKlageBehandling(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        ).synligeOpplysninger().filter { it.type in formkravOpplysningTyper }
            .forEach {
                oppdaterOpplysning(opplysningId = it.opplysningId, svar = Verdi.Boolsk(true))
            }
    }

    private fun KlageMediator.registrerOpplysningerMedUtfall(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
        utfall: UtfallType,
    ) {
        fun oppdaterOpplysning(
            opplysningId: UUID,
            svar: Verdi,
        ) {
            return oppdaterKlageOpplysning(
                behandlingId = behandlingId,
                opplysningId = opplysningId,
                verdi = svar,
                saksbehandler = saksbehandler,
            )
        }
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger().single { it.type == UTFALL }.opplysningId,
            svar = Verdi.TekstVerdi(utfall.name),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == VURDERING_AV_KLAGEN }.opplysningId,
            svar = Verdi.TekstVerdi("Dette er en vurdering."),
        )
    }

    private fun KlageMediator.registrerUtfallOpprettholdelseOpplysninger(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ) {
        fun oppdaterOpplysning(
            opplysningId: UUID,
            svar: Verdi,
        ) {
            return oppdaterKlageOpplysning(
                behandlingId = behandlingId,
                opplysningId = opplysningId,
                verdi = svar,
                saksbehandler = saksbehandler,
            )
        }
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger().single { it.type == UTFALL }.opplysningId,
            svar = Verdi.TekstVerdi(UtfallType.OPPRETTHOLDELSE.name),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == VURDERING_AV_KLAGEN }.opplysningId,
            svar = Verdi.TekstVerdi("Vi opprettholder vedtaket."),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == HVEM_KLAGER }.opplysningId,
            svar = Verdi.TekstVerdi(HvemKlagerType.BRUKER.name),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == HJEMLER }.opplysningId,
            svar = Verdi.Flervalg("FTRL_4_5_REGISTRERING", "FTRL_4_2"),
        )
    }
}
