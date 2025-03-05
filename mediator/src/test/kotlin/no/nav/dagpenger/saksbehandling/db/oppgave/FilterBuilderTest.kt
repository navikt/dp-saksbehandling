package no.nav.dagpenger.saksbehandling.db.oppgave

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FilterBuilderTest {
    @Test
    fun `Parser emneknagger riktig`() {
        FilterBuilder("emneknagg=A+B").emneknagg() shouldBe setOf("A B")
    }
}
