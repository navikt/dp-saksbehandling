package no.nav.dagpenger.saksbehandling.klage

import PersonMediator
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.audit.ApiAuditlogg
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.klage.PostgresKlageRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.hendelser.KlageBehandlingUtført
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HJEMLER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HVEM_KLAGER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.INTERN_MELDING
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST_OPPFYLT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER_VEDTAK
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.VURDERING_AV_KLAGEN
import no.nav.dagpenger.saksbehandling.klage.Verdi.Boolsk
import no.nav.dagpenger.saksbehandling.klage.Verdi.Flervalg
import no.nav.dagpenger.saksbehandling.klage.Verdi.TekstVerdi
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingType
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KlageOpprettholdtFlowTest {
    private val testPersonIdent = "12345678901"
    private val testRapid = TestRapid()
    private val saksbehandler =
        Saksbehandler(
            "saksbehandler",
            grupper = emptySet(),
            tilganger = setOf(SAKSBEHANDLER),
        )

    @Test
    fun `Komplett flow for klage med opprettholdelse sender til klageinstans`() {
        withMigratedDb { dataSource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val sakRepository = PostgresSakRepository(dataSource)
            val klageRepository = PostgresKlageRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)

            val personMediator =
                PersonMediator(
                    personRepository = personRepository,
                    pdlKlient =
                        mockk<PDLPersonIntern>().also {
                            coEvery { it.person(any()) } returns
                                PDLPerson(
                                    ident = testPersonIdent,
                                    fornavn = "Test",
                                    etternavn = "Person",
                                    mellomnavn = null,
                                    fødselsdato = null,
                                    alder = null,
                                    statsborgerskap = null,
                                    kjønn = null,
                                    adressebeskyttelseGradering = UGRADERT,
                                )
                        },
                )

            val sakMediator = SakMediator(sakRepository, personMediator)
            val oppgaveMediator = OppgaveMediator(oppgaveRepository)

            val meldingOmVedtakKlient =
                mockk<MeldingOmVedtakKlient>().also {
                    every { it.hentMeldingOmVedtak(any()) } returns null
                }

            val oppslag =
                mockk<Oppslag>().also {
                    coEvery { it.hentBehandler(any()) } returns
                        BehandlerDTO(
                            ident = saksbehandler.navIdent,
                            fornavn = "Test",
                            etternavn = "Saksbehandler",
                            enhet = BehandlerDTOEnhetDTO(enhetNr = "4450", navn = "Test enhet"),
                        )
                }

            val utsendingMediator = UtsendingMediator(mockk(relaxed = true), mockk(relaxed = true))
            utsendingMediator.setRapidsConnection(testRapid)

            val klageMediator =
                KlageMediator(
                    klageRepository = klageRepository,
                    oppgaveMediator = oppgaveMediator,
                    utsendingMediator = utsendingMediator,
                    meldingOmVedtakKlient = meldingOmVedtakKlient,
                    oppslag = oppslag,
                    sakMediator = sakMediator,
                )
            klageMediator.setRapidsConnection(testRapid)
            klageMediator.setAuditlogg(ApiAuditlogg(mockk(relaxed = true), testRapid))

            // 1. Opprett manuell klage
            val behandlingId =
                klageMediator
                    .opprettManuellKlage(
                        ManuellKlageMottattHendelse(
                            ident = testPersonIdent,
                            sakId = sakMediator.opprettEllerHentSak(testPersonIdent).id,
                            opprettet = LocalDateTime.now(),
                            journalpostId = "journalpostId",
                            utførtAv = saksbehandler,
                        ),
                    ).behandling.behandlingId

            // 2. Sett oppgaveansvar
            oppgaveMediator.settOppgaveAnsvar(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgaveMediator.hentOppgaveFor(behandlingId, saksbehandler).oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                ),
            )

            // 3. Fyll ut opplysninger
            val klageBehandling = klageMediator.hentKlageBehandling(behandlingId, saksbehandler)
            val vedtakId = UUID.randomUUID()

            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(KLAGEN_GJELDER_VEDTAK),
                TekstVerdi(vedtakId.toString()),
                saksbehandler,
            )
            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(KLAGEFRIST_OPPFYLT),
                Boolsk(true),
                saksbehandler,
            )
            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(VURDERING_AV_KLAGEN),
                TekstVerdi("Vurdert"),
                saksbehandler,
            )
            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(HVEM_KLAGER),
                Flervalg(listOf(HvemKlagerType.BRUKER.name)),
                saksbehandler,
            )
            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(HJEMLER),
                Flervalg(listOf("§4-2", "§4-3")),
                saksbehandler,
            )
            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(INTERN_MELDING),
                TekstVerdi("Intern melding til klageinstans"),
                saksbehandler,
            )
            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(UTFALL),
                Flervalg(listOf(UtfallType.OPPRETTHOLDT.name)),
                saksbehandler,
            )

            // 4. Ferdigstill behandling
            klageMediator.behandlingUtført(
                KlageBehandlingUtført(
                    behandlingId = behandlingId,
                    utførtAv = saksbehandler,
                ),
            )

            val oppdatertKlage = klageMediator.hentKlageBehandling(behandlingId, saksbehandler)
            oppdatertKlage.tilstand().type shouldBe BEHANDLES

            // 5. Simuler at utsending er distribuert
            klageMediator.vedtakDistribuert(
                UtsendingDistribuert(
                    behandlingId = behandlingId,
                    distribusjonId = "dist-123",
                    journalpostId = "jp-vedtak-ny",
                    utsendingId = UUID.randomUUID(),
                    ident = testPersonIdent,
                ),
            )

            // Verifiser tilstand
            val ferdigKlage = klageMediator.hentKlageBehandling(behandlingId, saksbehandler)
            ferdigKlage.tilstand().type shouldBe OVERSEND_KLAGEINSTANS

            // Verifiser at behov er publisert
            val behovMelding = testRapid.inspektør.message(testRapid.inspektør.size - 1)
            behovMelding.path("@behov").map { it.asText() }.toList() shouldBe listOf("OversendelseKlageinstans")
            behovMelding.path("behandlingId").asText() shouldBe behandlingId.toString()
            behovMelding.path("ident").asText() shouldBe testPersonIdent
            behovMelding.path("behandlendeEnhet").asText() shouldBe "4450"
            behovMelding.path("hjemler").map { it.asText() }.toList() shouldBe listOf("§4-2", "§4-3")
            behovMelding.path("kommentar").asText() shouldBe "Intern melding til klageinstans"
        }
    }

    @Test
    fun `OversendelseKlageinstans behov publiseres med korrekte journalposter`() {
        withMigratedDb { dataSource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val sakRepository = PostgresSakRepository(dataSource)
            val klageRepository = PostgresKlageRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)

            val personMediator =
                PersonMediator(
                    personRepository = personRepository,
                    pdlKlient =
                        mockk<PDLPersonIntern>().also {
                            coEvery { it.person(any()) } returns
                                PDLPerson(
                                    ident = testPersonIdent,
                                    fornavn = "Test",
                                    etternavn = "Person",
                                    mellomnavn = null,
                                    fødselsdato = null,
                                    alder = null,
                                    statsborgerskap = null,
                                    kjønn = null,
                                    adressebeskyttelseGradering = UGRADERT,
                                )
                        },
                )

            val sakMediator = SakMediator(sakRepository, personMediator)
            val oppgaveMediator = OppgaveMediator(oppgaveRepository)

            val meldingOmVedtakKlient =
                mockk<MeldingOmVedtakKlient>().also {
                    every { it.hentMeldingOmVedtak(any()) } returns null
                }

            val oppslag =
                mockk<Oppslag>().also {
                    coEvery { it.hentBehandler(any()) } returns
                        BehandlerDTO(
                            ident = saksbehandler.navIdent,
                            fornavn = "Test",
                            etternavn = "Saksbehandler",
                            enhet = BehandlerDTOEnhetDTO(enhetNr = "4450", navn = "Test enhet"),
                        )
                }

            val utsendingMediator = UtsendingMediator(mockk(relaxed = true), mockk(relaxed = true))
            utsendingMediator.setRapidsConnection(testRapid)

            val klageMediator =
                KlageMediator(
                    klageRepository = klageRepository,
                    oppgaveMediator = oppgaveMediator,
                    utsendingMediator = utsendingMediator,
                    meldingOmVedtakKlient = meldingOmVedtakKlient,
                    oppslag = oppslag,
                    sakMediator = sakMediator,
                )
            klageMediator.setRapidsConnection(testRapid)
            klageMediator.setAuditlogg(ApiAuditlogg(mockk(relaxed = true), testRapid))

            val behandlingId =
                klageMediator
                    .opprettManuellKlage(
                        ManuellKlageMottattHendelse(
                            ident = testPersonIdent,
                            sakId = sakMediator.opprettEllerHentSak(testPersonIdent).id,
                            opprettet = LocalDateTime.now(),
                            journalpostId = "journalpostId",
                            utførtAv = saksbehandler,
                        ),
                    ).behandling.behandlingId

            oppgaveMediator.settOppgaveAnsvar(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgaveMediator.hentOppgaveFor(behandlingId, saksbehandler).oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                ),
            )

            val klageBehandling = klageMediator.hentKlageBehandling(behandlingId, saksbehandler)
            val vedtakId = UUID.randomUUID()

            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(KLAGEN_GJELDER_VEDTAK),
                TekstVerdi(vedtakId.toString()),
                saksbehandler,
            )
            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(KLAGEFRIST_OPPFYLT),
                Boolsk(true),
                saksbehandler,
            )
            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(VURDERING_AV_KLAGEN),
                TekstVerdi("Vurdert"),
                saksbehandler,
            )
            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(HVEM_KLAGER),
                Flervalg(listOf(HvemKlagerType.BRUKER.name)),
                saksbehandler,
            )
            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(HJEMLER),
                Flervalg(listOf("§4-2")),
                saksbehandler,
            )
            klageMediator.oppdaterKlageOpplysning(
                behandlingId,
                klageBehandling.finnOpplysningId(UTFALL),
                Flervalg(listOf(UtfallType.OPPRETTHOLDT.name)),
                saksbehandler,
            )

            klageMediator.behandlingUtført(
                KlageBehandlingUtført(
                    behandlingId = behandlingId,
                    utførtAv = saksbehandler,
                ),
            )

            klageMediator.vedtakDistribuert(
                UtsendingDistribuert(
                    behandlingId = behandlingId,
                    distribusjonId = "dist-123",
                    journalpostId = "jp-klagemelding-ny",
                    utsendingId = UUID.randomUUID(),
                    ident = testPersonIdent,
                ),
            )

            val behovMelding = testRapid.inspektør.message(testRapid.inspektør.size - 1)
            val journalposter = behovMelding.path("tilknyttedeJournalposter")

            journalposter.size() shouldBe 1
            journalposter[0].path("type").asText() shouldBe "KLAGEMELDING"
            journalposter[0].path("journalpostId").asText() shouldBe "jp-klagemelding-ny"
        }
    }

    private fun KlageBehandling.finnOpplysningId(type: OpplysningType): UUID =
        this.hentAlleOpplysninger().first { it.opplysningType == type }.opplysningId
}
