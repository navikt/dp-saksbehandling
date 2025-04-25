package no.nav.dagpenger.saksbehandling.db.klage

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import org.junit.jupiter.api.Test

class PostgresKlageRepositoryTest {
    @Test
    fun `Skal kunne lagre og hente klage behandlinger`() {
        val behandlingId = UUIDv7.ny()
        println("*** behandlingId: $behandlingId")

        withMigratedDb { ds ->
            val klageRepository = PostgresKlageRepository(ds)
            val klageBehandling = KlageBehandling(behandlingId = behandlingId)
            klageRepository.lagre(klageBehandling)

            val hentetKlageBehandling = klageRepository.hentKlageBehandling(behandlingId)

            hentetKlageBehandling.behandlingId shouldBe klageBehandling.behandlingId
            hentetKlageBehandling.alleOpplysninger().size shouldBe klageBehandling.alleOpplysninger().size
//            klageBehandling shouldBe hentetKlageBehandling
        }
    }
}
