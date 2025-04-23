package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.klage.InmemoryKlageRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillKlageOppgave
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.klage.Datatype
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

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
                    klageRepository = InmemoryKlageRepository,
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

            oppgaveMediator.hentOppgaveFor(behandlingId)
                .tilstand().type shouldBe Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING

            oppgaveMediator.hentOppgaveFor(behandlingId).let {
                oppgaveMediator.tildelOppgave(
                    settOppgaveAnsvarHendelse =
                        SettOppgaveAnsvarHendelse(
                            oppgaveId = it.oppgaveId,
                            ansvarligIdent = saksbehandler.navIdent,
                            utførtAv = saksbehandler,
                        ),
                )
            }

            val klageBehandling = klageMediator.hentKlageBehandling(behandlingId)
            klageBehandling.opprettholdelse()

            klageMediator.ferdigstill(
                FerdigstillKlageOppgave(
                    utførtAv = saksbehandler,
                    behandlingId = klageBehandling.behandlingId,
                ),
            )

            oppgaveMediator.hentOppgaveFor(behandlingId).tilstand().type shouldBe Oppgave.Tilstand.Type.FERDIG_BEHANDLET
            // todo sjekke tilstand til klagebehandling

            verify(exactly = 1) {
                utsendingMediator.opprettUtsending(
                    oppgaveId = oppgaveMediator.hentOppgaveFor(behandlingId).oppgaveId,
                    brev = "må hente html",
                    ident = oppgaveMediator.hentOppgaveFor(behandlingId).behandling.person.ident,
                )
            }
            verify(exactly = 1) {
                utsendingMediator.mottaStartUtsending(any())
            }
        }
    }

    private fun KlageBehandling.opprettholdelse() {
        while (!this.kanFerdigstilles()) {
            val list = this.synligeOpplysninger().filter { it.verdi == Verdi.TomVerdi && it.type.påkrevd }
            list.forEach {
                when (it.type.datatype) {
                    Datatype.TEKST -> this.svar(it.id, "svar")
                    Datatype.DATO -> this.svar(it.id, LocalDate.MIN)
                    Datatype.BOOLSK -> this.svar(it.id, false)
                    Datatype.FLERVALG -> this.svar(it.id, listOf("1", "2"))
                }
            }
        }
    }
}
