package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.OpplysningType.KLAGEFRIST_OPPFYLT
import org.junit.jupiter.api.Test

class StegTest {
    @Test
    fun `Opprett fristvurdering steg`() {
        val steg = FristvurderingSteg()

        val klagefristOppfylt = steg.opplysninger().single { it.type == KLAGEFRIST_OPPFYLT }
        klagefristOppfylt.svar(verdi = true)
        steg.opplysninger().size shouldBe 3
        (klagefristOppfylt.verdi as Verdi.Boolsk).value shouldBe true
        klagefristOppfylt.svar(verdi = false)
        steg.opplysninger().size shouldBe 5
        (klagefristOppfylt.verdi as Verdi.Boolsk).value shouldBe false
    }
}
