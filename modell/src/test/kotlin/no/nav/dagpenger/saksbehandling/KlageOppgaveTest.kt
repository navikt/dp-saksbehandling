package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.KlageOppgave.Opprettet
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KlageOppgaveTest {
    @Test
    fun `Opprettelse av KlageOppgave har tilstand Opprettet`() {
        KlageOppgave(
            oppgaveId = UUIDv7.ny(),
            opprettet = LocalDateTime.now(),
        ).tilstand() shouldBe Opprettet
    }
}
