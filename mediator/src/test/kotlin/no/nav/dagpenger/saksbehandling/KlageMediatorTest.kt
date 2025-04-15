package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.db.klage.InmemoryKlageRepository
import no.nav.dagpenger.saksbehandling.db.klage.InmemoryKlageRepository.testKlageId1
import org.junit.jupiter.api.Test

class KlageMediatorTest {
    @Test
    fun `hent klage returnerer en dummyklage`() {
        val klageMediator =
            KlageMediator(
                klageRepository = InmemoryKlageRepository,
                personRepository = mockk(),
                oppslag = mockk(),
            )
        val klageOppgave = klageMediator.hentKlageOppgave(testKlageId1)
        klageOppgave.oppgaveId shouldBe testKlageId1
    }
}
