package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.klage.PostgresKlageRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillKlageOppgave
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.klage.HvemKlagerType
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KlageMediatorTest {
    private val testPersonIdent = "12345678901"

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

    @Test
    fun `Livssyklus til en klage`() {
        val saksbehandler = Saksbehandler("saksbehandler", grupper = emptySet())
        val utsendingMediator = mockk<UtsendingMediator>(relaxed = true)
        withMigratedDb { ds ->
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = PostgresPersonRepository(datasource = ds),
                    oppgaveRepository = PostgresOppgaveRepository(ds),
                    behandlingKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                    oppslag = oppslagMock,
                    meldingOmVedtakKlient = mockk<MeldingOmVedtakKlient>(),
                )

            val klageMediator =
                KlageMediator(
                    klageRepository = PostgresKlageRepository(ds),
                    oppgaveMediator = oppgaveMediator,
                    utsendingMediator = utsendingMediator,
                )

            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                )

            val oppgaveKlarTilBehandling =
                oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)
            oppgaveKlarTilBehandling.tilstand().type shouldBe Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING

            oppgaveMediator.tildelOppgave(
                settOppgaveAnsvarHendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgaveKlarTilBehandling.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
            )

            val klageBehandling = klageMediator.hentKlageBehandling(behandlingId)
            klageMediator.svarBehandlingsopplysninger(behandlingId, saksbehandler)

            shouldThrow<IllegalStateException> {
                klageMediator.ferdigstill(
                    FerdigstillKlageOppgave(
                        utførtAv = saksbehandler,
                        behandlingId = behandlingId,
                    ),
                )
            }
            // klageBehandling.synligeOpplysninger()
            klageMediator.svarUtfallOpprettholdelse(behandlingId, saksbehandler)

            klageMediator.ferdigstill(
                FerdigstillKlageOppgave(
                    utførtAv = saksbehandler,
                    behandlingId = behandlingId,
                ),
            )

            val ferdigbehandletOppgave =
                oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)
            ferdigbehandletOppgave.tilstand().type shouldBe Oppgave.Tilstand.Type.FERDIG_BEHANDLET
            // todo sjekke tilstand til klagebehandling

            verify(exactly = 1) {
                utsendingMediator.opprettUtsending(
                    oppgaveId = ferdigbehandletOppgave.oppgaveId,
                    brev = "må hente html",
                    ident = ferdigbehandletOppgave.behandling.person.ident,
                )
            }
            verify(exactly = 1) {
                utsendingMediator.mottaStartUtsending(any())
            }
        }
    }

    private fun KlageMediator.svarBehandlingsopplysninger(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ) {
        fun oppdaterOpplysning(
            opplysningId: UUID,
            svar: OpplysningerVerdi,
        ) {
            return oppdaterKlageOpplysning(
                behandlingId = behandlingId,
                opplysningId = opplysningId,
                verdi = svar,
                saksbehandler = saksbehandler,
            )
        }
        // OpplysnigerVerdi kan være TEKST, BOOLSK, DATO eller FLERVALG

        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId,
                ).synligeOpplysninger().single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAK }.id,
            svar = OpplysningerVerdi.Tekst("klagen gjelder vedtak"),
        )

        oppdaterOpplysning(
            opplysningId = this.hentKlageBehandling(behandlingId).synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST }.id,
            svar = OpplysningerVerdi.Dato(LocalDate.MIN),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId,
                ).synligeOpplysninger().single { it.type == OpplysningType.KLAGE_MOTTATT }.id,
            svar = OpplysningerVerdi.Dato(LocalDate.MIN),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId,
                ).synligeOpplysninger().single { it.type == OpplysningType.KLAGEFRIST_OPPFYLT }.id,
            svar = OpplysningerVerdi.Boolsk(true),
        )
        this.hentKlageBehandling(behandlingId).synligeOpplysninger().filter { it.type in formkravOpplysningTyper }.forEach {
            oppdaterOpplysning(opplysningId = it.id, svar = OpplysningerVerdi.Boolsk(true))
        }
    }

    private fun KlageMediator.svarUtfallOpprettholdelse(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ) {
        fun oppdaterOpplysning(
            opplysningId: UUID,
            svar: OpplysningerVerdi,
        ) {
            return oppdaterKlageOpplysning(
                behandlingId = behandlingId,
                opplysningId = opplysningId,
                verdi = svar,
                saksbehandler = saksbehandler,
            )
        }
        oppdaterOpplysning(
            opplysningId = this.hentKlageBehandling(behandlingId).synligeOpplysninger().single { it.type == OpplysningType.UTFALL }.id,
            svar = OpplysningerVerdi.Tekst(UtfallType.OPPRETTHOLDELSE.name),
        )
        oppdaterOpplysning(
            opplysningId =
                this.hentKlageBehandling(
                    behandlingId,
                ).synligeOpplysninger().single { it.type == OpplysningType.VURDERNIG_AV_KLAGEN }.id,
            svar = OpplysningerVerdi.Tekst("Vi opprettholder vedtaket."),
        )
        oppdaterOpplysning(
            opplysningId = this.hentKlageBehandling(behandlingId).synligeOpplysninger().single { it.type == OpplysningType.HVEM_KLAGER }.id,
            svar = OpplysningerVerdi.Tekst(HvemKlagerType.BRUKER.name),
        )
        oppdaterOpplysning(
            opplysningId = this.hentKlageBehandling(behandlingId).synligeOpplysninger().single { it.type == OpplysningType.HJEMLER }.id,
            svar = OpplysningerVerdi.TekstListe("§ 4-5", "§ 4-2"),
        )
    }
}
