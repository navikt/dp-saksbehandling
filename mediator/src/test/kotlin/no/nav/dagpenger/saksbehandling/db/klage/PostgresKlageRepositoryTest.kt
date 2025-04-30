package no.nav.dagpenger.saksbehandling.db.klage

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.klage.Datatype
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.BehandlingTilstand.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.Verdi
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PostgresKlageRepositoryTest {
    @Test
    fun `Skal kunne lagre og hente klagebehandlinger`() {
        val behandlingId = UUIDv7.ny()

        withMigratedDb { ds ->
            val klageRepository = PostgresKlageRepository(ds)
            val klageBehandling =
                KlageBehandling(
                    behandlingId = behandlingId,
                )

            val boolskOpplysningId =
                klageBehandling.finnEnBoolskOpplysning().also {
                    klageBehandling.svar(it, true)
                }

            val datoOpplysningerId =
                klageBehandling.finnEnDatoOpplysningerId().also {
                    klageBehandling.svar(it, LocalDate.MIN)
                }

            val listeOpplysningId =
                klageBehandling.finnEnListeOpplysningId().also {
                    klageBehandling.svar(it, listOf("String1", "String2"))
                }

            val tekstOpplysningId =
                klageBehandling.finnEnStringOpplysningId().also {
                    klageBehandling.svar(it, "String")
                }

            val boolskOpplysningMedTomVerdi =
                klageBehandling.synligeOpplysninger().first {
                    it.type.datatype == Datatype.BOOLSK && it.verdi() is Verdi.TomVerdi
                }.opplysningId

            klageRepository.lagre(klageBehandling)

            val hentetKlageBehandling = klageRepository.hentKlageBehandling(behandlingId)

            hentetKlageBehandling.behandlingId shouldBe klageBehandling.behandlingId
            hentetKlageBehandling.tilstand() shouldBe BEHANDLES
            hentetKlageBehandling.alleOpplysninger() shouldContainExactly klageBehandling.alleOpplysninger()

            hentetKlageBehandling.finnEnOpplysning(boolskOpplysningId).verdi() shouldBe Verdi.Boolsk(true)
            hentetKlageBehandling.finnEnOpplysning(datoOpplysningerId).verdi() shouldBe Verdi.Dato(LocalDate.MIN)
            hentetKlageBehandling.finnEnOpplysning(listeOpplysningId).verdi() shouldBe Verdi.Flervalg(listOf("String1", "String2"))
            hentetKlageBehandling.finnEnOpplysning(tekstOpplysningId).verdi() shouldBe Verdi.TekstVerdi("String")
            hentetKlageBehandling.finnEnOpplysning(boolskOpplysningMedTomVerdi).verdi() shouldBe Verdi.TomVerdi
        }
    }

    private fun KlageBehandling.finnEnOpplysning(id: UUID): Opplysning {
        return this.alleOpplysninger().single { opplysning -> opplysning.opplysningId == id }
    }

    private fun KlageBehandling.finnEnBoolskOpplysning(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.BOOLSK }.opplysningId
    }

    private fun KlageBehandling.finnEnStringOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.TEKST }.opplysningId
    }

    private fun KlageBehandling.finnEnDatoOpplysningerId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.DATO }.opplysningId
    }

    private fun KlageBehandling.finnEnListeOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.FLERVALG }.opplysningId
    }
}
