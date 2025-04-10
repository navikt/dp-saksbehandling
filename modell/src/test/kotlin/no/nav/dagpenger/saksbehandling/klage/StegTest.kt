package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST_OPPFYLT
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.klagenGjelderOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.lagOpplysninger
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.utfallOpplysningTyper
import org.junit.jupiter.api.Test

class StegTest {
    @Test
    fun `Opprett klagen-gjelder-steg og verifiser synlighet`() {
        val steg = KlagenGjelderSteg
        val opplysninger =
            lagOpplysninger(klagenGjelderOpplysningTyper)
        steg.evaluerSynlighet(opplysninger)

        opplysninger.filter { opplysning ->
            opplysning.type in klagenGjelderOpplysningTyper
        }.forEach { it.synlighet() shouldBe true }
    }

    @Test
    fun `Opprett fristvurdering-steg og verifiser synlighet`() {
        val steg = FristvurderingSteg
        val opplysninger =
            lagOpplysninger(
                fristvurderingOpplysningTyper + oversittetFristOpplysningTyper,
            )
        val klagefristOppfylt = opplysninger.single { it.type == KLAGEFRIST_OPPFYLT }

        steg.evaluerSynlighet(opplysninger.toList())
        klagefristOppfylt.svar(verdi = true)
        steg.evaluerSynlighet(opplysninger.toList())

        (klagefristOppfylt.verdi as Verdi.Boolsk).value shouldBe true
        opplysninger.filter { opplysning ->
            opplysning.type in oversittetFristOpplysningTyper
        }.forEach { it.synlighet() shouldBe false }

        klagefristOppfylt.svar(verdi = false)
        steg.evaluerSynlighet(opplysninger.toList())

        (klagefristOppfylt.verdi as Verdi.Boolsk).value shouldBe false
        opplysninger.filter { opplysning ->
            opplysning.type in oversittetFristOpplysningTyper
        }.forEach { it.synlighet() shouldBe true }
    }

    @Test
    fun `Opprett formkrav-steg og verifiser synlighet`() {
        val steg = FormkravSteg
        val opplysninger =
            lagOpplysninger(
                klagenGjelderOpplysningTyper +
                    fristvurderingOpplysningTyper +
                    oversittetFristOpplysningTyper +
                    formkravOpplysningTyper,
            )
        steg.evaluerSynlighet(opplysninger.toList())
        opplysninger.filter { opplysning ->
            opplysning.type in formkravOpplysningTyper
        }.forEach { it.synlighet() shouldBe true }
    }

    @Test
    fun `Opprett utfall-steg og verifiser synlighet`() {
        val steg = VurderUtfallSteg

        val opplysninger =
            lagOpplysninger(
                formkravOpplysningTyper + utfallOpplysningTyper,
            )

        steg.evaluerSynlighet(opplysninger.toList())
        opplysninger.filter { opplysning ->
            opplysning.type in utfallOpplysningTyper
        }.forEach { it.synlighet() shouldBe false }

        opplysninger.filter { opplysning ->
            opplysning.type in formkravOpplysningTyper
        }.forEach { it.svar(verdi = true) }
        steg.evaluerSynlighet(opplysninger.toList())

        opplysninger.filter { opplysning ->
            opplysning.type in utfallOpplysningTyper
        }.forEach { it.synlighet() shouldBe true }
    }
}
