package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.OpplysningType.KLAGEFRIST_OPPFYLT
import no.nav.dagpenger.saksbehandling.OpplysningerBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.OpplysningerBygger.lagOpplysninger
import no.nav.dagpenger.saksbehandling.OpplysningerBygger.oversittetFristOpplysningTyper
import org.junit.jupiter.api.Test

class StegTest {
    @Test
    fun `Opprett fristvurdering steg`() {
        val steg = FristvurderingSteg()

        val opplysninger =
            lagOpplysninger(
                oversittetFristOpplysningTyper + fristvurderingOpplysningTyper,
            )

        val klagefristOppfylt = opplysninger.single { it.type == KLAGEFRIST_OPPFYLT }
        steg.reevaluerOpplysngninger(opplysninger.toList())
        klagefristOppfylt.svar(verdi = true)
        steg.reevaluerOpplysngninger(opplysninger.toList())
        (klagefristOppfylt.verdi as Verdi.Boolsk).value shouldBe true
        opplysninger.filter { it.type in oversittetFristOpplysningTyper }
            .forEach { it.synlighet() shouldBe false }
    }
}
