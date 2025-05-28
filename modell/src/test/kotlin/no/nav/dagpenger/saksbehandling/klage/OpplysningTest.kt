package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.lagOpplysninger
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpplysningTest {
    @Test
    fun `skal kun kunne sette valgmuligheter der de eksisterer`() {
        lagOpplysninger(opplysninger = setOf(OpplysningType.UTFALL)).single().let {
            shouldThrow<UgyldigOpplysningException> { it.svar(Verdi.TekstVerdi("feil")) }
            shouldThrow<UgyldigOpplysningException> { it.svar(Verdi.TekstVerdi("opprettholdelse")) }
            shouldNotThrowAny { it.svar(Verdi.TekstVerdi("OPPRETTHOLDELSE")) }
        }
    }

    @Test
    fun `skal evaluere regler`() {
        lagOpplysninger(opplysninger = setOf(OpplysningType.KLAGEN_GJELDER_VEDTAKSDATO)).single().let {
            shouldThrow<UgyldigOpplysningException> { it.svar(Verdi.Dato(LocalDate.MAX)) }
            shouldNotThrowAny { it.svar(Verdi.Dato(LocalDate.MIN)) }
        }
    }

    @Test
    fun `sjekk at flervalg godtar flervalg`() {
        lagOpplysninger(opplysninger = setOf(OpplysningType.HJEMLER)).single().let {
            shouldNotThrowAny { it.svar(Verdi.Flervalg(listOf("FTRL_4_4", "FTRL_4_2"))) }
        }
    }
}
