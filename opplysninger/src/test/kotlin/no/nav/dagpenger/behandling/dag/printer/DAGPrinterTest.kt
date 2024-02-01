package no.nav.dagpenger.behandling.dag.printer

import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelsett
import no.nav.dagpenger.behandling.dag.RegeltreBygger
import no.nav.dagpenger.behandling.regel.multiplikasjon
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DAGPrinterTest {
    private val regelsett = Regelsett()
    private val a = Opplysningstype<Double>("A")
    private val b = Opplysningstype<Double>("B")
    private val c =
        Opplysningstype<Double>("A * B").also {
            regelsett.multiplikasjon(it, a, b)
        }

    private val regeltre = RegeltreBygger(regelsett.regler()).dag()

    @Test
    fun `test av PrettyPrinter`() {
        val prettyPrinter = PrettyPrinter(regeltre)
        //language=text
        assertEquals(
            """
            A * B: Opplysningstype(navn='A * B', parent=null, child=0)
              | Multiplikasjon
              A: Opplysningstype(navn='A', parent=null, child=0)
              | Multiplikasjon
              B: Opplysningstype(navn='B', parent=null, child=0)
            """.trimIndent(),
            prettyPrinter.toPrint(),
        )
    }

    @Test
    fun `test av MermaidPrinter`() {
        val mermaidPrinter = MermaidPrinter(regeltre)
        assertTrue(mermaidPrinter.toPrint().contains("graph RL"))
        /*assertEquals(
        // language=mermaid
            """
            graph RL
              1806431167["A * B"] -->|Multiplikasjon| 1297836716["A"]
              1806431167["A * B"] -->|Multiplikasjon| 710190911["B"]
            """.trimIndent(),
            mermaidPrinter.toPrint(),
        )*/
    }
}
