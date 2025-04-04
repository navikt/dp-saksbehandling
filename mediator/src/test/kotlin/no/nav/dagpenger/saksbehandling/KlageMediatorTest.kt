package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDTO.Type.TEKST
import org.junit.jupiter.api.Test

class KlageMediatorTest {
    @Test
    fun `hent klage returnerer en dummyklage`() {
        val klageMediator = KlageMediator()
        val klageId = UUIDv7.ny()
        val klage = klageMediator.hentKlage(klageId)
        klage.id shouldBe klageId
    }
}
