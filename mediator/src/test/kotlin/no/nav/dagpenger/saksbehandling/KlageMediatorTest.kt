package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.klage.PostgresKlageRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.klage.HvemKlagerType
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.BehandlingTilstand.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.KlageHttpKlient
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HJEMLER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HVEM_KLAGER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST_OPPFYLT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER_VEDTAK
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGE_MOTTATT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.VURDERING_AV_KLAGEN
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KlageMediatorTest {
    private val testPersonIdent = "12345678901"
    private val klageKlient = mockk<KlageHttpKlient>()

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
    fun `Livssyklus til en klage som ferdigstilles`() {
        val saksbehandler = Saksbehandler("saksbehandler", grupper = emptySet())
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
                    klageKlient = klageKlient,
                )

            val behandlingId =
                klageMediator.opprettKlage(
                    KlageMottattHendelse(
                        ident = testPersonIdent,
                        opprettet = LocalDateTime.now(),
                        journalpostId = "journalpostId",
                    ),
                ).behandling.behandlingId

            klageMediator.hentKlageBehandling(behandlingId, saksbehandler).tilstand() shouldBe
                KlageBehandling.BehandlingTilstand.BEHANDLES

            val oppgave = oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)

            oppgave.tilstand().type shouldBe Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING

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
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                )
            }

            klageMediator.registrerUtfallOpprettholdelseOpplysninger(behandlingId, saksbehandler)

            klageMediator.ferdigstill(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            )

            klageMediator.hentKlageBehandling(behandlingId = behandlingId, saksbehandler = saksbehandler)
                .tilstand() shouldBe FERDIGSTILT

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
    fun `Livssyklus til en klage som avbrytes`() {
        val saksbehandler = Saksbehandler("saksbehandler", grupper = emptySet())
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
                    klageKlient = klageKlient,
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
            ).tilstand() shouldBe
                KlageBehandling.BehandlingTilstand.BEHANDLES

            val oppgave = oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)

            oppgave.tilstand().type shouldBe Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING

            oppgaveMediator.tildelOppgave(
                settOppgaveAnsvarHendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
            )

            klageMediator.avbrytKlage(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            )

            klageMediator.hentKlageBehandling(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ).tilstand() shouldBe
                KlageBehandling.BehandlingTilstand.AVBRUTT

            oppgaveMediator.hentOppgaveFor(behandlingId = behandlingId, saksbehandler = saksbehandler)
                .tilstand().type shouldBe FERDIG_BEHANDLET
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
