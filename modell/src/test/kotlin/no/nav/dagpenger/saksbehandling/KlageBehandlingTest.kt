package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class KlageBehandlingTest {
    @Test
    fun `opprett klagebehandling`() {
        val klageBehandling =
            KlageBehandling(
                id = java.util.UUID.randomUUID(),
                person =
                    Person(
                        ident = "12345678901",
                        skjermesSomEgneAnsatte = false,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                    ),
            )

        klageBehandling.grupper.map { it.navn }.toSet() shouldBe GrupperNavn.entries.toSet()

        val opplysninngId = klageBehandling.opplysninger.single { it.navn == "Er klagen skriftlig" }.id

        klageBehandling.svar(opplysninngId, false)
        klageBehandling.utfall shouldBe Utfall.Avvist
    }

    @Test
    fun `Skal kunne svare på opplysninger av ulike typer`() {
        val klageBehandling =
            KlageBehandling(
                id = java.util.UUID.randomUUID(),
                person =
                    Person(
                        ident = "12345678901",
                        skjermesSomEgneAnsatte = false,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                    ),
            )

        val boolskOpplysningId = klageBehandling.finnEnBoolskOpplysning()
        val stringOpplysningId = klageBehandling.finnEnStringOpplysningId()
        val datoOpplysningId = klageBehandling.finnEnDatoOpplysningerId()
        val listeOpplysningId = klageBehandling.finnEnListeOpplysningId()

        klageBehandling.svar(boolskOpplysningId, false)
        klageBehandling.hentOpplysning(boolskOpplysningId).verdi.let {
            require(it is Verdi.Boolsk)
            it.value shouldBe false
        }

        klageBehandling.svar(stringOpplysningId, "String")
        klageBehandling.hentOpplysning(stringOpplysningId).verdi.let {
            require(it is Verdi.TekstVerdi)
            it.value shouldBe "String"
        }

        klageBehandling.svar(datoOpplysningId, LocalDate.MIN)
        klageBehandling.hentOpplysning(datoOpplysningId).verdi.let {
            require(it is Verdi.Dato)
            it.value shouldBe LocalDate.MIN
        }

        klageBehandling.svar(listeOpplysningId, listOf("String1", "String2"))
        klageBehandling.hentOpplysning(listeOpplysningId).verdi.let {
            require(it is Verdi.Flervalg)
            it.value shouldBe listOf("String1", "String2")
        }
    }

    @Test
    fun `Hvis utfall er opprettholdelse så skal tilhørende opplysninger vises`() {
    }

    private fun KlageBehandling.finnEnBoolskOpplysning(): UUID {
        return this.opplysninger.first { it.type == Opplysning.OpplysningType.BOOLSK }.id
    }

    private fun KlageBehandling.finnEnStringOpplysningId(): UUID {
        return this.opplysninger.first { it.type == Opplysning.OpplysningType.TEKST }.id
    }

    private fun KlageBehandling.finnEnDatoOpplysningerId(): UUID {
        return this.opplysninger.first { it.type == Opplysning.OpplysningType.DATO }.id
    }

    private fun KlageBehandling.finnEnListeOpplysningId(): UUID {
        return this.opplysninger.first { it.type == Opplysning.OpplysningType.FLERVALG }.id
    }
}
