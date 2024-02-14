package no.nav.dagpenger.saksbehandling.graph

import no.nav.dagpenger.saksbehandling.Steg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StegVisualisererVisitorTest {
    private val bror = Steg.fastsettelse<Int>("bror")
    private val søster = Steg.fastsettelse<Int>("søster")
    private val fetter = Steg.Vilkår("fetter")
    private val kusine = Steg.Vilkår("kusine")

    private val mor =
        Steg.Vilkår("mor").also {
            it.avhengerAv(bror)
            it.avhengerAv(søster)
        }

    private val onkel =
        Steg.fastsettelse<Int>("onkel").also {
            it.avhengerAv(fetter)
            it.avhengerAv(kusine)
        }

    private val bestefar =
        Steg.fastsettelse<Int>("bestefar").also {
            it.avhengerAv(mor)
            it.avhengerAv(onkel)
        }

    private val oldemor =
        Steg.Vilkår("oldemor").also {
            it.avhengerAv(bestefar)
        }

    @Test
    fun `Skal kunne generere mermaid-syntax`() {
        val visitor = StegVisualisererVisitor()
        oldemor.accept(visitor)
        assertEquals(expectedDiagram, visitor.asDiagram())
    }

    @Test
    fun `Skal kunne generere en markdown-fil med et embeddet marmaid-diagram`() {
        val visitor = StegVisualisererVisitor()
        oldemor.accept(visitor)
        assertEquals(expectedFileContents, visitor.toMarkdownFile(writeToFile = false))
    }

    private val expectedDiagram = // language=mermaid
        """
        |graph TD
        |    oldemor --> bestefar
        |    bestefar --> mor
        |    bestefar --> onkel
        |    mor --> bror
        |    mor --> søster
        |    onkel --> fetter
        |    onkel --> kusine
        """.trimMargin()

    private val expectedFileContents =
        """
        |```mermaid
        |$expectedDiagram
        |```
        """.trimMargin()
}
