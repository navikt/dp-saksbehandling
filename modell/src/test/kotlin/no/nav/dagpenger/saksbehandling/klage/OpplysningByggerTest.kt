package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test
import java.util.UUID

class OpplysningByggerTest {
    @Test
    fun `lag opplysninger med behandlingIder som kan påklages`() {
        val behandlingIderSomKanPåklages = setOf(UUID.randomUUID(), UUID.randomUUID())
        val opplysninger =
            OpplysningBygger.lagOpplysninger(
                opplysninger = OpplysningType.entries.toSet(),
                behandlingIderSomKanPåklages = behandlingIderSomKanPåklages,
            )

        val klageGjelderOpplysning = opplysninger.single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAK }

        klageGjelderOpplysning.valgmuligheter shouldContainExactlyInAnyOrder behandlingIderSomKanPåklages.map(UUID::toString)
    }
}
