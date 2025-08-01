package no.nav.dagpenger.saksbehandling

import PersonMediator
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.audit.ApiAuditlogg
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.klage.PostgresKlageRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresRepository
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.klage.HvemKlagerType
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_1
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_2
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_3
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_LAND
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_NAVN
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_POSTNR
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_POSTSTED
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HJEMLER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HVEM_KLAGER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.INTERN_MELDING
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
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KlageMediatorTest {
    private val testPersonIdent = "12345678901"
    private val testRapid = TestRapid()
    private val saksbehandler = Saksbehandler("saksbehandler", grupper = emptySet())
    private val pdlPersonIntern =
        PDLPersonIntern(
            ident = testPersonIdent,
            fornavn = "eruditi",
            etternavn = "persius",
            mellomnavn = null,
            fødselsdato = LocalDate.MIN,
            alder = 4463,
            statsborgerskap = null,
            kjønn = PDLPerson.Kjonn.UKJENT,
            adresseBeskyttelseGradering = UGRADERT,
            sikkerhetstiltak = listOf(),
        )
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
            val person =
                Person(
                    id = UUIDv7.ny(),
                    ident = testPersonIdent,
                    skjermesSomEgneAnsatte = false,
                    adressebeskyttelseGradering = UGRADERT,
                )
            coEvery { it.hentPersonMedSkjermingOgAdressebeskyttelse(testPersonIdent) } returns person
            coEvery { it.hentPerson(testPersonIdent) } returns
                pdlPersonIntern
            coEvery { it.hentBehandler(ident = any()) } returns behandlerDTO
        }
    private val html = "<html>hei</html>"
    private val meldingOmVedtakKlientMock =
        mockk<MeldingOmVedtakKlient>().also {
            coEvery {
                it.lagOgHentMeldingOmVedtak(
                    person = pdlPersonIntern,
                    saksbehandler = behandlerDTO,
                    beslutter = null,
                    behandlingId = any(),
                    saksbehandlerToken = any(),
                    behandlingType = BehandlingType.KLAGE,
                )
            } returns Result.success(html)
        }
    private val auditloggMock =
        mockk<ApiAuditlogg>().also {
            coEvery {
                it.opprett(
                    melding = any(),
                    ident = any(),
                    saksbehandler = any(),
                )
            } returns Unit
            coEvery {
                it.oppdater(
                    melding = any(),
                    ident = any(),
                    saksbehandler = any(),
                )
            } returns Unit
            coEvery {
                it.les(
                    melding = any(),
                    ident = any(),
                    saksbehandler = any(),
                )
            } returns Unit
            coEvery {
                it.slett(
                    melding = any(),
                    ident = any(),
                    saksbehandler = any(),
                )
            } returns Unit
        }

    @Test
    fun `Livssyklus til en digitalt mottatt klage som ferdigstilles med opprettholdelse`() {
        setupMediatorerOgSak { klageMediator, oppgaveMediator, sakId ->

            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        sakId = sakId,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                ).behandlingId

            klageMediator.hentKlageBehandling(behandlingId, saksbehandler).tilstand().type shouldBe BEHANDLES

            val oppgave = oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)

            oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING
            oppgave.tilstandslogg.size shouldBe 1

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
                    hendelse =
                        KlageFerdigbehandletHendelse(
                            behandlingId = behandlingId,
                            utførtAv = saksbehandler,
                        ),
                    saksbehandlerToken = "token",
                )
            }

            klageMediator.registrerUtfallOpprettholdelseOpplysninger(behandlingId, saksbehandler)
            klageMediator.ferdigstill(
                hendelse =
                    KlageFerdigbehandletHendelse(
                        behandlingId = behandlingId,
                        utførtAv = saksbehandler,
                    ),
                saksbehandlerToken = "token",
            )

            val klageBehandling =
                klageMediator.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                )
            klageBehandling.tilstand().type shouldBe OVERSEND_KLAGEINSTANS
            klageBehandling.behandlendeEnhet() shouldBe behandlerDTO.enhet.enhetNr

            OversendtKlageinstansMottak(
                rapidsConnection = testRapid,
                klageMediator = klageMediator,
            )

            val løsningPåBehovForOversendelseTilKA =
                """
                {
                  "@event_name" : "behov",
                  "@behov" : [ "OversendelseKlageinstans" ],
                  "behandlingId" : "$behandlingId",
                  "ident" : "${oppgave.personIdent()}",
                  "fagsakId" : "$sakId",
                  "behandlendeEnhet": "${klageBehandling.behandlendeEnhet()}",
                  "hjemler": ${klageBehandling.hjemler().map { "\"$it\"" }},
                  "@løsning": {
                    "OversendelseKlageinstans": "OK"
                  }
                }
                """.trimIndent()

            testRapid.sendTestMessage(
                key = oppgave.personIdent(),
                message = løsningPåBehovForOversendelseTilKA,
            )

            klageMediator.hentKlageBehandling(behandlingId = behandlingId, saksbehandler = saksbehandler)
                .tilstand().type shouldBe FERDIGSTILT

            oppgaveMediator.hentOppgaveFor(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand().type shouldBe FERDIG_BEHANDLET

            testRapid.inspektør.size shouldBe 2
            testRapid.inspektør.message(0).behovNavn() shouldBe "SaksbehandlingPdfBehov"
            testRapid.inspektør.message(1).behovNavn() shouldBe "OversendelseKlageinstans"

            // i hver /registrer\w+?Opplysninger/-funksjon leses klagebehandlingen for hver opplysning som legges på.
            // dermed blir tallene for både lesing og oppdatering sykt store.
            verify(exactly = 21) { auditloggMock.les("Så en klagebehandling", testPersonIdent, saksbehandler.navIdent) }
            verify(exactly = 21) { auditloggMock.oppdater("Oppdaterte en klageopplysning", testPersonIdent, saksbehandler.navIdent) }
            verify(exactly = 1) { auditloggMock.oppdater("Ferdigstilte en klage", testPersonIdent, saksbehandler.navIdent) }
        }
    }

    @Test
    fun `Livssyklus til en manuell klage som ferdigstilles med opprettholdelse`() {
        setupMediatorerOgSak { klageMediator, oppgaveMediator, sakId ->

            val behandlingId =
                klageMediator.opprettManuellKlage(
                    ManuellKlageMottattHendelse(
                        ident = testPersonIdent,
                        sakId = sakId,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                        utførtAv = saksbehandler,
                    ),
                ).behandlingId

            klageMediator.hentKlageBehandling(behandlingId, saksbehandler).tilstand().type shouldBe BEHANDLES

            val oppgave = oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)

            oppgave.tilstand().type shouldBe UNDER_BEHANDLING
            oppgave.behandlerIdent shouldBe saksbehandler.navIdent
            oppgave.tilstandslogg.size shouldBe 2
            oppgave.sisteSaksbehandler() shouldBe saksbehandler.navIdent

            klageMediator.registrerKlageBehandlingOpplysninger(behandlingId, saksbehandler)

            shouldThrow<IllegalStateException> {
                klageMediator.ferdigstill(
                    hendelse =
                        KlageFerdigbehandletHendelse(
                            behandlingId = behandlingId,
                            utførtAv = saksbehandler,
                        ),
                    saksbehandlerToken = "token",
                )
            }

            klageMediator.registrerUtfallOpprettholdelseOpplysninger(behandlingId, saksbehandler)
            klageMediator.ferdigstill(
                hendelse =
                    KlageFerdigbehandletHendelse(
                        behandlingId = behandlingId,
                        utførtAv = saksbehandler,
                    ),
                saksbehandlerToken = "token",
            )
            val klageBehandling =
                klageMediator.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                )
            klageBehandling.tilstand().type shouldBe OVERSEND_KLAGEINSTANS
            klageBehandling.behandlendeEnhet() shouldBe behandlerDTO.enhet.enhetNr

            OversendtKlageinstansMottak(
                rapidsConnection = testRapid,
                klageMediator = klageMediator,
            )

            val løsningPåBehovForOversendelseTilKA =
                """
                {
                  "@event_name" : "behov",
                  "@behov" : [ "OversendelseKlageinstans" ],
                  "behandlingId" : "$behandlingId",
                  "ident" : "${oppgave.personIdent()}",
                  "fagsakId" : "$sakId",
                  "behandlendeEnhet": "${klageBehandling.behandlendeEnhet()}",
                  "hjemler": ${klageBehandling.hjemler().map { "\"$it\"" }},
                  "@løsning": {
                    "OversendelseKlageinstans": "OK"
                  }
                }
                """.trimIndent()

            testRapid.sendTestMessage(
                key = oppgave.personIdent(),
                message = løsningPåBehovForOversendelseTilKA,
            )

            klageMediator.hentKlageBehandling(behandlingId = behandlingId, saksbehandler = saksbehandler)
                .tilstand().type shouldBe FERDIGSTILT
            oppgaveMediator.hentOppgaveFor(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand().type shouldBe FERDIG_BEHANDLET

            testRapid.inspektør.size shouldBe 2
            testRapid.inspektør.message(0).behovNavn() shouldBe "SaksbehandlingPdfBehov"
            testRapid.inspektør.message(1).behovNavn() shouldBe "OversendelseKlageinstans"
        }
    }

    @Test
    fun `Livssyklus til en klage som ferdigstilles med avvisning`() {
        setupMediatorerOgSak { klageMediator, oppgaveMediator, sakId ->

            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        sakId = sakId,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                ).behandlingId

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
                    hendelse =
                        KlageFerdigbehandletHendelse(
                            behandlingId = behandlingId,
                            utførtAv = saksbehandler,
                        ),
                    saksbehandlerToken = "token",
                )
            }

            klageMediator.registrerOpplysningerMedUtfall(behandlingId, saksbehandler, UtfallType.AVVIST)
            klageMediator.ferdigstill(
                hendelse =
                    KlageFerdigbehandletHendelse(
                        behandlingId = behandlingId,
                        utførtAv = saksbehandler,
                    ),
                saksbehandlerToken = "token",
            )
            val klageBehandling =
                klageMediator.hentKlageBehandling(behandlingId = behandlingId, saksbehandler = saksbehandler)
            klageBehandling.tilstand().type shouldBe FERDIGSTILT
            klageBehandling.utfall() shouldBe UtfallType.AVVIST
            klageBehandling.behandlendeEnhet() shouldBe "440Gakk"
            testRapid.inspektør.size shouldBe 1
            testRapid.inspektør.message(0).behovNavn() shouldBe "SaksbehandlingPdfBehov"
        }
    }

    @Test
    fun `Livssyklus til en klage som avbrytes`() {
        setupMediatorerOgSak { klageMediator, oppgaveMediator, sakId ->
            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        sakId = sakId,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                ).behandlingId

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
            testRapid.inspektør.size shouldBe 0
        }
    }

    @Test
    fun `Kan ikke ferdigstille en klage med medhold`() {
        setupMediatorerOgSak { klageMediator, oppgaveMediator, sakId ->
            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        sakId = sakId,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                ).behandlingId

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
                    hendelse =
                        KlageFerdigbehandletHendelse(
                            behandlingId = behandlingId,
                            utførtAv = saksbehandler,
                        ),
                    saksbehandlerToken = "token",
                )
            }

            oppgaveMediator.hentOppgaveFor(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand().type shouldBe UNDER_BEHANDLING
            testRapid.inspektør.size shouldBe 0
        }
    }

    @Test
    fun `Kan ikke ferdigstille en klage med delvis medhold`() {
        setupMediatorerOgSak { klageMediator, oppgaveMediator, sakId ->
            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        sakId = sakId,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                ).behandlingId

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
                    hendelse =
                        KlageFerdigbehandletHendelse(
                            behandlingId = behandlingId,
                            utførtAv = saksbehandler,
                        ),
                    saksbehandlerToken = "token",
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
            svar = Verdi.TekstVerdi(utfall.tekst),
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
            svar = Verdi.TekstVerdi(UtfallType.OPPRETTHOLDELSE.tekst),
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
            svar = Verdi.TekstVerdi(HvemKlagerType.FULLMEKTIG.tekst),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == HJEMLER }.opplysningId,
            svar =
                Verdi.Flervalg(
                    "§ 4-5 Krav til å være registrert som arbeidssøker for å være reell arbeidssøker",
                    "§ 4-2 Krav til opphold i Norge",
                ),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == INTERN_MELDING }.opplysningId,
            svar = Verdi.TekstVerdi("nice"),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == FULLMEKTIG_NAVN }.opplysningId,
            svar = Verdi.TekstVerdi("Djevelens Advokat"),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == FULLMEKTIG_ADRESSE_1 }.opplysningId,
            svar = Verdi.TekstVerdi("Sydenveien 1"),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == FULLMEKTIG_ADRESSE_2 }.opplysningId,
            svar = Verdi.TekstVerdi("Poste restante"),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == FULLMEKTIG_ADRESSE_3 }.opplysningId,
            svar = Verdi.TekstVerdi("Teisen postkontor"),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == FULLMEKTIG_POSTNR }.opplysningId,
            svar = Verdi.TekstVerdi("0666"),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == FULLMEKTIG_POSTSTED }.opplysningId,
            svar = Verdi.TekstVerdi("Oslo"),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                ).synligeOpplysninger()
                    .single { it.type == FULLMEKTIG_LAND }.opplysningId,
            svar = Verdi.TekstVerdi("Norge"),
        )
    }

    private fun setupMediatorerOgSak(test: (KlageMediator, OppgaveMediator, UUID) -> Unit) {
        withMigratedDb { dataSource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val personMediator = PersonMediator(personRepository = personRepository, oppslagMock)

            val sakMediator =
                SakMediator(
                    personMediator = personMediator,
                    sakRepository = PostgresRepository(dataSource = dataSource),
                ).also {
                    it.setRapidsConnection(rapidsConnection = testRapid)
                }
            val utsendingMediator =
                UtsendingMediator(
                    repository = PostgresUtsendingRepository(dataSource),
                ).also { it.setRapidsConnection(rapidsConnection = testRapid) }

            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = PostgresOppgaveRepository(dataSource),
                    behandlingKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                    oppslag = oppslagMock,
                    meldingOmVedtakKlient = meldingOmVedtakKlientMock,
                    sakMediator = sakMediator,
                )
            val klageMediator =
                KlageMediator(
                    klageRepository = PostgresKlageRepository(dataSource),
                    oppgaveMediator = oppgaveMediator,
                    utsendingMediator = utsendingMediator,
                    oppslag = oppslagMock,
                    meldingOmVedtakKlient = meldingOmVedtakKlientMock,
                    sakMediator = sakMediator,
                ).also {
                    it.setAuditlogg(auditlogg = auditloggMock)
                    it.setRapidsConnection(rapidsConnection = testRapid)
                }
            personRepository.lagre(
                Person(
                    ident = testPersonIdent,
                    skjermesSomEgneAnsatte = false,
                    adressebeskyttelseGradering = UGRADERT,
                ),
            )
            val sak =
                sakMediator.opprettSak(
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = UUIDv7.ny(),
                        behandlingId = UUIDv7.ny(),
                        ident = testPersonIdent,
                        opprettet = LocalDateTime.now(),
                    ),
                )

            test(klageMediator, oppgaveMediator, sak.sakId)
        }
    }

    private fun JsonNode.behovNavn(): String {
        require(this["@event_name"].asText() == "behov") { "Forventet behov som event_name" }
        return this["@behov"].single().asText()
    }
}
