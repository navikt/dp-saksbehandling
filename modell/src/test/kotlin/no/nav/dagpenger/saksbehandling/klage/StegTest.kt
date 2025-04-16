package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.fullmektigTilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.klagenGjelderOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.lagOpplysninger
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.tilKlageinstansOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.utfallOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST_OPPFYLT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import org.junit.jupiter.api.Test

class StegTest {
    @Test
    fun `Opprett steg for å angi hva klagen gjelder og verifiser synlighet`() {
        val steg = KlagenGjelderSteg
        val opplysninger =
            lagOpplysninger(klagenGjelderOpplysningTyper)
        steg.evaluerSynlighet(opplysninger)

        opplysninger.filter { opplysning ->
            opplysning.type in klagenGjelderOpplysningTyper
        }.forEach { it.synlighet() shouldBe true }
    }

    @Test
    fun `Opprett steg for fristvurdering og verifiser synlighet`() {
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
    fun `Opprett steg for formkrav og verifiser synlighet`() {
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
    fun `Opprett steg for utfall og verifiser synlighet`() {
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

    @Test
    fun `Opprett steg for oversendelse til klageinstans og verifiser synlighet`() {
        val steg = OversendKlageinstansSteg
        val opplysninger =
            lagOpplysninger(
                utfallOpplysningTyper + tilKlageinstansOpplysningTyper,
            )
        val utfallOpplysning =
            opplysninger.single { opplysning ->
                opplysning.type == UTFALL
            }
        val tilKlageinstansOpplysninger =
            opplysninger.filter { opplysning ->
                opplysning.type in tilKlageinstansOpplysningTyper
            }

        steg.evaluerSynlighet(opplysninger.toList())
        tilKlageinstansOpplysninger.forEach { it.synlighet() shouldBe false }

        utfallOpplysning.svar(verdi = UtfallType.MEDHOLD.name)
        steg.evaluerSynlighet(opplysninger.toList())
        tilKlageinstansOpplysninger.forEach { it.synlighet() shouldBe false }

        utfallOpplysning.svar(verdi = UtfallType.DELVIS_MEDHOLD.name)
        steg.evaluerSynlighet(opplysninger.toList())
        tilKlageinstansOpplysninger.forEach { it.synlighet() shouldBe false }

        utfallOpplysning.svar(verdi = UtfallType.AVVIST.name)
        steg.evaluerSynlighet(opplysninger.toList())
        tilKlageinstansOpplysninger.forEach { it.synlighet() shouldBe false }

        utfallOpplysning.svar(verdi = UtfallType.OPPRETTHOLDELSE.name)
        steg.evaluerSynlighet(opplysninger.toList())
        tilKlageinstansOpplysninger.forEach { it.synlighet() shouldBe true }
    }

    @Test
    fun `Opprett steg for fullmektig og verifiser synlighet`() {
        val opplysninger =
            lagOpplysninger(
                utfallOpplysningTyper + tilKlageinstansOpplysningTyper + fullmektigTilKlageinstansOpplysningTyper,
            )

        val utfallOpplysning =
            opplysninger.single { opplysning ->
                opplysning.type == UTFALL
            }
        val hvemKlager = opplysninger.single { it.type == OpplysningType.HVEM_KLAGER }

        val fullmektigTilKlageinstansOpplysninger = opplysninger.filter { it.type in fullmektigTilKlageinstansOpplysningTyper }
        fullmektigTilKlageinstansOpplysninger.size shouldBe fullmektigTilKlageinstansOpplysningTyper.size

        utfallOpplysning.svar(verdi = UtfallType.OPPRETTHOLDELSE.name)
        FullmektigSteg.evaluerSynlighet(opplysninger.toList())
        fullmektigTilKlageinstansOpplysninger.forEach { it.synlighet() shouldBe false }

        hvemKlager.svar(HvemKlagerType.FULLMEKTIG.name)
        FullmektigSteg.evaluerSynlighet(opplysninger.toList())
        fullmektigTilKlageinstansOpplysninger.forEach { it.synlighet() shouldBe true }

        fullmektigTilKlageinstansOpplysninger.forEach { it.svar("en tekst verdi") }

        hvemKlager.svar(HvemKlagerType.BRUKER.name)
        FullmektigSteg.evaluerSynlighet(opplysninger.toList())
        fullmektigTilKlageinstansOpplysninger.forEach {
            it.synlighet() shouldBe false
            it.verdi shouldBe Verdi.TomVerdi
        }
    }
}
