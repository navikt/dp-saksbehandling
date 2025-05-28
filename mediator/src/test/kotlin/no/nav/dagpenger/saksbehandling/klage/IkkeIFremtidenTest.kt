package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import java.time.LocalDate

class IkkeIFremtidenTest {
    @Test
    fun `Regel er kun gyldig dersom opplysningens verdi er en dato tilbake i tid`() {
        val iDag = LocalDate.now()
        shouldThrow<UgyldigOpplysningException> { lagOpplysning(iDag.plusDays(1)) }

        shouldThrow<UgyldigOpplysningException> {
            lagOpplysning(iDag.minusDays(1)).also {
                it.svar(Verdi.Dato(iDag.plusDays(1)))
            }
        }

        shouldNotThrowAny {
            lagOpplysning(iDag.minusDays(1))
        }

        shouldNotThrowAny {
            lagOpplysning(iDag)
        }

        shouldNotThrowAny {
            // tom verdi er gyldig
            lagOpplysning(null)
        }

        shouldNotThrowAny {
            lagOpplysning(iDag).also { it.svar(Verdi.TomVerdi) }
        }
    }

    private fun lagOpplysning(dato: LocalDate?): Opplysning {
        return Opplysning(
            type = OpplysningType.KLAGEN_GJELDER_VEDTAKSDATO,
            verdi = dato?.let { Verdi.Dato(it) } ?: Verdi.TomVerdi,
            regler = setOf(IkkeIFremtiden),
        )
    }
}
