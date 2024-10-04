package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TilstandsloggTest {
    private val now = LocalDateTime.now()

    private val hendelseIdag =
        Tilstandsendring(
            tilstand = OPPRETTET,
            hendelse = TomHendelse,
            tidspunkt = now,
        )

    private val hendelseIGår =
        Tilstandsendring(
            tilstand = OPPRETTET,
            hendelse = TomHendelse,
            tidspunkt = now.minusDays(1),
        )

    private val hendelseImorgen =
        Tilstandsendring(
            tilstand = OPPRETTET,
            hendelse = TomHendelse,
            tidspunkt = now.plusDays(1),
        )

    @Test
    fun `elementer sorteres etter tidspunkt, nyeste først`() {
        Tilstandslogg.rehydrer(listOf(hendelseIdag, hendelseImorgen, hendelseIGår)).let {
            it shouldBe (listOf(hendelseImorgen, hendelseIdag, hendelseIGår))
        }
    }

    @Test
    fun `elementer legges til starten av listen`() {
        Tilstandslogg().let { logg ->
            logg.leggTil(OPPRETTET, TomHendelse)
            logg.leggTil(KLAR_TIL_KONTROLL, TomHendelse)
            logg.leggTil(UNDER_BEHANDLING, TomHendelse)

            logg.map { it.tilstand } shouldBe listOf(UNDER_BEHANDLING, KLAR_TIL_KONTROLL, OPPRETTET)
        }
    }
}
