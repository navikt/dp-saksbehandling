package no.nav.dagpenger.behandling.hendelser

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.DefaultOppgaveVisitor
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSporing
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.Steg
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadInnsendtHendelseTest {
    val søknadInnsendtHendelse = SøknadInnsendtHendelse(UUID.randomUUID(), "123", "12345678910")
    val person = Person("12345678910").also { it.håndter(søknadInnsendtHendelse) }

    private val behandling = søknadInnsendtHendelse.oppgave(person).let {
        DefaultOppgaveVisitor(it).behandling
    }

    @Test
    fun `Ikke ferdig`() {
        behandling.erFerdig() shouldBe false
        behandling.utfall() shouldBe false
    }

    @Test
    fun `Ikke ferdig fordi vilkåret er oppfylt, men resten av stegene må utføres`() {
        (behandling.steg.single { it.id == "Oppfyller kravene til dagpenger" } as Steg.Vilkår).besvar(true, testSporing)
        behandling.erFerdig() shouldBe false
        behandling.utfall() shouldBe true
    }

    @Test
    fun `Ferdig fordi vilkåret ikke er oppfylt`() {
        (behandling.steg.single { it.id == "Oppfyller kravene til dagpenger" } as Steg.Vilkår).besvar(
            false,
            testSporing,
        )
        behandling.erFerdig() shouldBe true
        behandling.utfall() shouldBe false
    }
}
