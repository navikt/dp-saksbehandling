package no.nav.dagpenger.behandling.dag

import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelsett
import no.nav.dagpenger.behandling.regel.multiplikasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegeltreByggerTest {
    @Test
    fun `bygg regeltre`() {
        val regelsett = Regelsett()
        val a = Opplysningstype<Double>("A")
        val b = Opplysningstype<Double>("B")
        val c = Opplysningstype<Double>("A * B")
        regelsett.multiplikasjon(c, a, b)

        val regeltre = RegeltreBygger(regelsett.regler()).dag()
        assertEquals(3, regeltre.nodes.size, "Har en node for hver opplysningstype")
        assertEquals(2, regeltre.edges.size, "Har en kobling fra utledet til avhengig opplysningstype")
    }
}
