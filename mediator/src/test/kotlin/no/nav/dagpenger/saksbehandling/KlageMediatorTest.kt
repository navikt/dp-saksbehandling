package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.db.klage.InmemoryKlageRepository.testKlageId1
import org.junit.jupiter.api.Test

class KlageMediatorTest {
    @Test
    fun `hent klage returnerer en dummyklage`() {
        val klageMediator = KlageMediator()
        val klage = klageMediator.hentKlage(testKlageId1)
        klage.id shouldBe testKlageId1
    }
}
