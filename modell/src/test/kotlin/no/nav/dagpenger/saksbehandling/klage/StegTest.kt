package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST_OPPFYLT
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.lagOpplysninger
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.utfallOpplysningTyper
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
        steg.reevaluerOpplysninger(opplysninger.toList())
        klagefristOppfylt.svar(verdi = true)
        steg.reevaluerOpplysninger(opplysninger.toList())
        (klagefristOppfylt.verdi as Verdi.Boolsk).value shouldBe true
        opplysninger.filter { it.type in oversittetFristOpplysningTyper }
            .forEach { it.synlighet() shouldBe false }
    }

    @Test
    fun `Opprett utfallsteg og verifiser synlighet`() {
        val steg = VurderUtfallSteg()

        val opplysninger =
            lagOpplysninger(
                utfallOpplysningTyper + formkravOpplysningTyper,
            )

        steg.reevaluerOpplysninger(opplysninger.toList())
        opplysninger.filter { it.type in utfallOpplysningTyper }
            .forEach { it.synlighet() shouldBe false }
        opplysninger.filter { it.type in formkravOpplysningTyper }.forEach {
            it.svar(verdi = true)
        }
        steg.reevaluerOpplysninger(opplysninger.toList())
        opplysninger.filter { it.type in utfallOpplysningTyper }
            .forEach { it.synlighet() shouldBe true }
    }
}
