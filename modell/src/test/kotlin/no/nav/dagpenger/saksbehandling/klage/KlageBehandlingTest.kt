package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.klagenGjelderOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.utfallOpplysningTyper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class KlageBehandlingTest {
    @Test
    fun `Skal kunne svare og endre på opplysninger av ulike typer`() {
        val klageBehandling =
            KlageBehandling(
                behandlingId = UUIDv7.ny(),
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
    fun `Hvis alle behandlingsopplysninger er utfylt - skal utfall kunne velges`() {
        val klageBehandling =
            KlageBehandling(
                behandlingId = UUID.randomUUID(),
            )
        klageBehandling.synligeOpplysninger().filter { opplysning ->
            opplysning.type in utfallOpplysningTyper &&
                opplysning.synlighet()
        } shouldBe emptySet()

        // Besvarer alle opplysninger som er synlige, unntatt formkrav
        klageBehandling.synligeOpplysninger().filter { opplysning ->
            opplysning.type in klagenGjelderOpplysningTyper +
                fristvurderingOpplysningTyper +
                oversittetFristOpplysningTyper
        }.forEach {
            when (it.type.datatype) {
                Datatype.BOOLSK -> klageBehandling.svar(it.id, true)
                Datatype.TEKST -> klageBehandling.svar(it.id, "String")
                Datatype.DATO -> klageBehandling.svar(it.id, LocalDate.MIN)
                Datatype.FLERVALG -> klageBehandling.svar(it.id, listOf("String1", "String2"))
            }
        }

        klageBehandling.synligeOpplysninger().filter { opplysning ->
            opplysning.type in utfallOpplysningTyper &&
                opplysning.synlighet()
        } shouldBe emptySet()

        // Besvarer formkrav
        klageBehandling.synligeOpplysninger().filter { opplysning ->
            opplysning.type in formkravOpplysningTyper
        }.forEach {
            klageBehandling.svar(it.id, true)
        }

        klageBehandling.synligeOpplysninger().filter { opplysning ->
            opplysning.type in utfallOpplysningTyper &&
                opplysning.synlighet()
        } shouldNotBe emptySet<Opplysning>()
    }

    private fun KlageBehandling.finnEnOpplysning(opplysningType: OpplysningType): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type == opplysningType }.id
    }

    private fun KlageBehandling.finnEnBoolskOpplysning(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.BOOLSK }.id
    }

    private fun KlageBehandling.finnEnStringOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.TEKST }.id
    }

    private fun KlageBehandling.finnEnDatoOpplysningerId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.DATO }.id
    }

    private fun KlageBehandling.finnEnListeOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.FLERVALG }.id
    }
}
