package no.nav.dagpenger.behandling.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Faktum
import no.nav.dagpenger.behandling.Opplysninger
import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelmotor
import no.nav.dagpenger.behandling.Regelsett
import org.junit.jupiter.api.Test

internal class EnAvRegelTest {
    private val opplysningB = Opplysningstype<Boolean>("B")
    private val opplysningC = Opplysningstype<Boolean>("C")
    private val produserer = Opplysningstype<Boolean>("A")
    private val opplysninger =
        Opplysninger(
            Regelmotor(
                Regelsett().also {

                    it.enAvRegel(
                        produserer,
                        opplysningB,
                        opplysningC,
                    )
                },
            ),
            mutableListOf(),
        )
    private val regel =
        EnAvRegel(Opplysningstype("A"), opplysningB, opplysningC)

    @Test
    fun `hvis en av opplysningene er sanne så er utledningen sann`() {
        opplysninger.leggTil(Faktum(opplysningB, false))
        opplysninger.leggTil(Faktum(opplysningC, true))
        val utledet = opplysninger.finnOpplysning(produserer)
        utledet.verdi shouldBe true
    }

    @Test
    fun `hvis ingen av opplysningene er sanne så er utledningen usann`() {
        opplysninger.leggTil(Faktum(opplysningB, false))
        opplysninger.leggTil(Faktum(opplysningC, false))
        val utledet = opplysninger.finnOpplysning(produserer)
        utledet.verdi shouldBe false
    }

    @Test
    fun `hvis begge opplysningene er sanne så er utledningen sann`() {
        opplysninger.leggTil(Faktum(opplysningB, true))
        opplysninger.leggTil(Faktum(opplysningC, true))
        val utledet = opplysninger.finnOpplysning(produserer)
        utledet.verdi shouldBe true
        utledet.verdi shouldBe true
    }
}
