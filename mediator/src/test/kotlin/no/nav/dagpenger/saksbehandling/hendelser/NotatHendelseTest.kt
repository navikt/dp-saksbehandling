package no.nav.dagpenger.saksbehandling.hendelser

import io.kotest.assertions.throwables.shouldThrow
import no.nav.dagpenger.saksbehandling.Saksbehandler
import org.junit.jupiter.api.Test
import java.util.UUID

class NotatHendelseTest {
    @Test
    fun `notat kan ikke ha tom tekst`() {
        shouldThrow<IllegalArgumentException> {
            NotatHendelse(
                oppgaveId = UUID.randomUUID(),
                tekst = "",
                utførtAv = Saksbehandler("Z999999", emptySet(), emptySet()),
            )
        }
    }
}
