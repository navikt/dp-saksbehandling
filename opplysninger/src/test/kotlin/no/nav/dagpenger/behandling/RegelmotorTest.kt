package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.regel.enAvRegel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegelmotorTest {
    @Test
    fun `Regelsett kan ikke inneholder flere regler som produserer samme opplysningstype`() {
        val regelsett =
            Regelsett().apply {
                enAvRegel(Opplysningstype("A"), Opplysningstype("B"))
                enAvRegel(Opplysningstype("A"), Opplysningstype("C"))
            }

        assertThrows<IllegalArgumentException> {
            Regelmotor(regelsett)
        }
    }
}
