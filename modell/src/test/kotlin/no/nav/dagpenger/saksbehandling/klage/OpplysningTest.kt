package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.lagOpplysninger
import org.junit.jupiter.api.Test

class OpplysningTest {
    @Test
    fun `skal kun kunne sette valgmuligheter der de eksisterer`() {
        lagOpplysninger(opplysninger = setOf(OpplysningType.UTFALL)).single().let {
            shouldThrow<IllegalArgumentException> { it.svar(Verdi.TekstVerdi("feil")) }
            shouldThrow<IllegalArgumentException> { it.svar(Verdi.TekstVerdi("opprettholdelse")) }
            shouldNotThrowAny { it.svar(Verdi.TekstVerdi("OPPRETTHOLDELSE")) }
        }
    }

    @Test
    fun `sjekk at flervalg godtar flervalg`() {
        lagOpplysninger(opplysninger = setOf(OpplysningType.HJEMLER)).single().let {
            shouldNotThrowAny { it.svar(Verdi.Flervalg(listOf("§ 4-1", "§ 4-2"))) }
        }
    }
}
