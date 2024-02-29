package no.nav.dagpenger.saksbehandling.hendelser

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class SøknadsbehandlingOpprettetHendelseTest {
    private val ident = "12345612345"
    private val person = Person(ident)
    private val behandlingId = UUIDv7.ny()

    @Test
    fun `BehandlingOpprettetHendelse skal føre til at person opprettes med en behandling`() {
        val søknadsbehandlingOpprettetHendelse =
            SøknadsbehandlingOpprettetHendelse(
                søknadId = UUIDv7.ny(),
                behandlingId = behandlingId,
                ident = ident,
                opprettet = ZonedDateTime.now(),
            )
        person.håndter(søknadsbehandlingOpprettetHendelse)

        person.ident shouldBe ident
        person.behandlinger.size shouldBe 1
        person.behandlinger.get(behandlingId) shouldNotBe null
    }
}
