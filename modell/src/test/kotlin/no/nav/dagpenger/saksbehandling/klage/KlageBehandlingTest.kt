package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.ER_KLAGEN_SKRIFTLIG
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.ER_KLAGEN_UNDERSKREVET
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class KlageBehandlingTest {
    @Test
    fun `Skal kunne svare og endre på opplysninger av ulike typer`() {
        val klageBehandling =
            KlageBehandling(
                id = UUIDv7.ny(),
                person =
                    testPerson(),
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
                id = UUID.randomUUID(),
                person = testPerson(),
            )
        klageBehandling.synligeOpplysninger().filter { opplysning ->
            opplysning.type in setOf(ER_KLAGEN_SKRIFTLIG, ER_KLAGEN_UNDERSKREVET)
        }.forEach {
            klageBehandling.svar(it.id, true)
        }

        klageBehandling.hentUtfallOpplysninger() shouldNotBe emptySet<Opplysning>()
    }

    @Test
    fun `Utfall er synlig når foregående steg er utfylt`() {
        val person =
            Person(
                ident = "12345612345",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            )
        val klageBehandling = KlageBehandling(person = person)
        klageBehandling.synligeOpplysninger()
            .filter { it.type in OpplysningerBygger.utfallOpplysningTyper }.size shouldBe 2
    }

    @Test
    fun `Utfall er ikke synlig når behandlingsopplysninger ikke er utfylt`() {
        val person =
            Person(
                ident = "12345612345",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            )
        val klageBehandling = KlageBehandling(person = person)
        klageBehandling.synligeOpplysninger()
            .filter { it.type in OpplysningerBygger.utfallOpplysningTyper }.size shouldBe 0
    }

    private fun testPerson(): Person =
        Person(
            ident = "12345678901",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = UGRADERT,
        )

    private fun KlageBehandling.finnEnOpplysning(template: OpplysningType): UUID {
        return this.synligeOpplysninger().first { it.type == template }.id
    }

    private fun KlageBehandling.finnEnBoolskOpplysning(): UUID {
        return this.synligeOpplysninger().first { it.type.datatype == Datatype.BOOLSK }.id
    }

    private fun KlageBehandling.finnEnStringOpplysningId(): UUID {
        return this.synligeOpplysninger().first { it.type.datatype == Datatype.TEKST }.id
    }

    private fun KlageBehandling.finnEnDatoOpplysningerId(): UUID {
        return this.synligeOpplysninger().first { it.type.datatype == Datatype.DATO }.id
    }

    private fun KlageBehandling.finnEnListeOpplysningId(): UUID {
        return this.synligeOpplysninger().first { it.type.datatype == Datatype.FLERVALG }.id
    }
}
