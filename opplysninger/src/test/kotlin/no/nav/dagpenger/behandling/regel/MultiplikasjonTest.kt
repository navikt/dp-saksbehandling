package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.Faktum
import no.nav.dagpenger.behandling.Opplysninger
import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelkjøring
import no.nav.dagpenger.behandling.Regelsett
import no.nav.dagpenger.behandling.mai
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MultiplikasjonTest {
    private val sum =
        Opplysningstype<Double>("SUM")
    private val a =
        Opplysningstype<Double>("A")
    private val b =
        Opplysningstype<Double>("B")

    private val opplysninger =
        Opplysninger(
            Regelkjøring(
                1.mai,
                Regelsett().also {
                    it.multiplikasjon(
                        sum,
                        a,
                        b,
                    )
                },
            ),
        )

    @Test
    fun `multiplikasjon regel`() {
        opplysninger.leggTil(Faktum(a, 2.0))
        opplysninger.leggTil(Faktum(b, 2.0))
        val utledet = opplysninger.finnOpplysning(sum)
        assertEquals(4.0, utledet.verdi)
    }
}
