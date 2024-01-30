package no.nav.dagpenger.behandling.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Faktum
import no.nav.dagpenger.behandling.Opplysningstype
import org.junit.jupiter.api.Test

internal class EnAvRegelTest {
    private val opplysningB = Opplysningstype<Boolean>("B")
    private val opplysningC = Opplysningstype<Boolean>("C")
    private val regel =
        EnAvRegel(Opplysningstype("A"), opplysningB, opplysningC)

    @Test
    fun `hvis en av opplysningene er sanne så er utledningen sann`() {
        val utledet =
            regel.lagProdukt(
                listOf(
                    Faktum(opplysningB, false),
                    Faktum(opplysningC, true),
                ),
            )
        utledet.verdi shouldBe true
    }

    @Test
    fun `hvis ingen av opplysningene er sanne så er utledningen usann`() {
        val utledet =
            regel.lagProdukt(
                listOf(
                    Faktum(opplysningB, false),
                    Faktum(opplysningC, false),
                ),
            )
        utledet.verdi shouldBe false
    }

    @Test
    fun `hvis begge opplysningene er sanne så er utledningen sann`() {
        val utledet =
            regel.lagProdukt(
                listOf(
                    Faktum(opplysningB, true),
                    Faktum(opplysningC, true),
                ),
            )
        utledet.verdi shouldBe true
    }
}
