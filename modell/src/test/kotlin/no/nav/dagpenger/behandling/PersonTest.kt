package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PersonTest {
    @Test
    fun `Alle hendelser skal havne på samme sak (Viggo case)`() {
        val person = Person("12345678910")
        person.håndter(
            SøknadInnsendtHendelse(
                søknadId = UUID.randomUUID(),
                journalpostId = "123",
                ident = "12345678910",
                innsendtDato = LocalDate.MIN,
            ),
        )
        person.håndter(
            SøknadInnsendtHendelse(
                søknadId = UUID.randomUUID(),
                journalpostId = "123",
                ident = "12345678910",
                innsendtDato = LocalDate.MIN,
            ),
        )

        val visitor = TestVisitor(person)
        visitor.saker.size shouldBe 1
    }
}

class TestVisitor(person: Person) : PersonVisitor {
    lateinit var saker: Set<Sak>

    init {
        person.accept(this)
    }

    override fun visit(saker: Set<Sak>) {
        this.saker = saker
    }
}
