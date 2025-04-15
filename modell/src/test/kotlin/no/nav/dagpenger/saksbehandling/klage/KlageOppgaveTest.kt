package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillKlageOppgave
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KlageOppgaveTest {
    private val saksbehandler =
        Saksbehandler(
            navIdent = "Z123",
            grupper = emptySet(),
            tilganger = emptySet(),
        )

    private val oppgaveId: UUID = UUIDv7.ny()
    private val testPerson =
        Person(
            ident = "12345612345",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )

    @Test
    fun `Livsyklus på klage oppgavens tilstander`() {
        val klageOppgave =
            KlageOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = LocalDateTime.now(),
                klageBehandling = KlageBehandling(person = testPerson),
            ).also {
                it.tilstand() shouldBe KlageOppgave.KlarTilBehandling
            }

        klageOppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler,
            ),
        )
        klageOppgave.tilstand() shouldBe KlageOppgave.UnderBehandling

        klageOppgave.ferdigstill(FerdigstillKlageOppgave(utførtAv = saksbehandler))
        klageOppgave.tilstand() shouldBe KlageOppgave.FerdigBehandlet
    }

    @Test
    fun `ulovlig tilstandsendring`() {
        shouldThrow<IllegalStateException> {
            KlageOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = LocalDateTime.now(),
                klageBehandling = KlageBehandling(person = testPerson),
            ).ferdigstill(FerdigstillKlageOppgave(utførtAv = saksbehandler))
        }

        val settOppgaveAnsvarHendelse =
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler,
            )
        val ferdigstiltKlageOppgave =
            KlageOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = LocalDateTime.now(),
                klageBehandling = KlageBehandling(person = testPerson),
            ).also {
                it.tildel(
                    settOppgaveAnsvarHendelse,
                )
                it.ferdigstill(FerdigstillKlageOppgave(utførtAv = saksbehandler))
            }
        shouldThrow<IllegalStateException> {
            ferdigstiltKlageOppgave.tildel(settOppgaveAnsvarHendelse)
        }
    }
}
