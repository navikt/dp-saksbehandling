package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.OpplysningTemplate.ER_KLAGEN_SKRIFTLIG
import no.nav.dagpenger.saksbehandling.OpplysningTemplate.ER_KLAGEN_UNDERSKREVET
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

        val opplysninngId = klageBehandling.hentOpplysninger().single { it.template == ER_KLAGEN_SKRIFTLIG }.id

        klageBehandling.svar(opplysninngId, false)
        klageBehandling.utfall shouldBe Utfall.Avvist
    }

    @Test
    fun `Skal kunne svare og endre på opplysninger av ulike typer`() {
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
        klageBehandling.svar(boolskOpplysningId, true)
        klageBehandling.hentOpplysning(boolskOpplysningId).verdi.let {
            require(it is Verdi.Boolsk)
            it.value shouldBe true
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
    fun `Hvis formkrav er utfylt skal utfall kunne velges`() {
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
        klageBehandling.hentOpplysninger().filter { it.template in setOf(ER_KLAGEN_SKRIFTLIG, ER_KLAGEN_UNDERSKREVET) }
            .forEach {
                klageBehandling.svar(it.id, true)
            }

        klageBehandling.hentUtfallOpplysninger() shouldNotBe emptySet<Opplysning>()
    }

    private fun KlageBehandling.finnEnOpplysning(template: OpplysningTemplate): UUID {
        return this.hentOpplysninger().first { it.template == template }.id
    }

    private fun KlageBehandling.finnEnBoolskOpplysning(): UUID {
        return this.hentOpplysninger().first { it.template.datatype == Opplysning.Datatype.BOOLSK }.id
    }

    private fun KlageBehandling.finnEnStringOpplysningId(): UUID {
        return this.hentOpplysninger().first { it.template.datatype == Opplysning.Datatype.TEKST }.id
    }

    private fun KlageBehandling.finnEnDatoOpplysningerId(): UUID {
        return this.hentOpplysninger().first { it.template.datatype == Opplysning.Datatype.DATO }.id
    }

    private fun KlageBehandling.finnEnListeOpplysningId(): UUID {
        return this.hentOpplysninger().first { it.template.datatype == Opplysning.Datatype.FLERVALG }.id
    }
}
