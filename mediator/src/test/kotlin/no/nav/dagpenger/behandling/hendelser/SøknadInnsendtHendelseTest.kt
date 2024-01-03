package no.nav.dagpenger.behandling.hendelser

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSporing
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.Steg
import no.nav.dagpenger.behandling.Utfall
import no.nav.dagpenger.behandling.helpers.DefaultOppgaveVisitor
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SøknadInnsendtHendelseTest {
    private val søknadInnsendtHendelse =
        SøknadInnsendtHendelse(
            søknadId = UUID.randomUUID(),
            journalpostId = "123",
            ident = "12345678910",
            innsendtDato = LocalDate.now(),
        )
    private val person = Person("12345678910").also { it.håndter(søknadInnsendtHendelse) }

    private val behandling =
        søknadInnsendtHendelse.oppgave(person).let {
            DefaultOppgaveVisitor(it).behandling
        }

    @Test
    fun `Ikke ferdig`() {
        behandling.erFerdig() shouldBe false
        behandling.utfall() shouldBe Utfall.Avslag
    }

    @Test
    fun `Ikke ferdig fordi vilkåret er oppfylt, men resten av stegene må utføres`() {
        (behandling.steg.single { it.id == "Oppfyller kravene til dagpenger" } as Steg.Vilkår).besvar(true, testSporing)
        behandling.erFerdig() shouldBe false
        behandling.utfall() shouldBe Utfall.Innvilgelse
    }

    @Test
    fun `Ferdig fordi vilkåret ikke er oppfylt`() {
        (behandling.steg.single { it.id == "Oppfyller kravene til dagpenger" } as Steg.Vilkår).besvar(
            false,
            testSporing,
        )
        // TODO: behandling.erFerdig() shouldBe true
        behandling.utfall() shouldBe Utfall.Avslag
    }
}
