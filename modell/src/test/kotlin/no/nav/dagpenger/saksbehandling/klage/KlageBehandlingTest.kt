package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.BehandlingTilstand.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.BehandlingTilstand.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.BehandlingTilstand.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.klagenGjelderOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.utfallOpplysningTyper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class KlageBehandlingTest {
    @Test
    fun `Skal kunne svare og endre på opplysninger med ulike datatyper`() {
        val klageBehandling =
            KlageBehandling(
                behandlingId = UUIDv7.ny(),
            )

        val boolskOpplysningId = klageBehandling.finnEnBoolskOpplysningId()
        val stringOpplysningId = klageBehandling.finnEnTekstOpplysningId()
        val datoOpplysningId = klageBehandling.finnEnDatoOpplysningId()
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
    fun `Utfall skal kunne velges når alle behandlingsopplysninger er utfylt`() {
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

    @Test
    fun `Klagebehandling kan ferdigstilles når alle synlige og påkrevde opplysninger er utfylt`() {
        val synligOgPåkrevdOpplysning =
            Opplysning(
                type = OpplysningType.KLAGEFRIST_OPPFYLT,
                verdi = Verdi.TomVerdi,
                synlig = true,
            )
        val ikkeSynligOpplysning =
            Opplysning(
                type = OpplysningType.FULLMEKTIG_LAND,
                verdi = Verdi.TomVerdi,
                synlig = false,
            )

        val ikkePåkrevdOpplysning =
            Opplysning(
                type = OpplysningType.FULLMEKTIG_ADRESSE_3,
                verdi = Verdi.TomVerdi,
                synlig = true,
            )
        val klageBehandling =
            KlageBehandling(
                steg = emptyList(),
                opplysninger = setOf(synligOgPåkrevdOpplysning, ikkePåkrevdOpplysning, ikkeSynligOpplysning),
            )
        klageBehandling.tilstand() shouldBe BEHANDLES

        shouldThrow<IllegalStateException> { klageBehandling.ferdigstill() }

        klageBehandling.svar(synligOgPåkrevdOpplysning.id, false)

        shouldNotThrow<IllegalStateException> { klageBehandling.ferdigstill() }
        klageBehandling.tilstand() shouldBe FERDIGSTILT
    }

    @Test
    fun `Klagebehandling skal kunne avbrytes fra tilstand BEHANDLES`() {
        val klageBehandling = KlageBehandling()
        klageBehandling.tilstand() shouldBe BEHANDLES

        klageBehandling.avbryt()
        klageBehandling.tilstand() shouldBe AVBRUTT
    }

    @Test
    fun `Klagebehandling skal ikke kunne avbrytes fra tilstand FERDIGSTILT`() {
        val klageBehandling = KlageBehandling()
        svarPåAlleOpplysninger(klageBehandling)
        klageBehandling.ferdigstill()

        shouldThrow<IllegalStateException> { klageBehandling.avbryt() }
    }

    private fun svarPåAlleOpplysninger(klageBehandling: KlageBehandling) {
        klageBehandling.alleOpplysninger().forEach {
            when (it.type.datatype) {
                Datatype.BOOLSK -> klageBehandling.svar(it.id, true)
                Datatype.TEKST -> klageBehandling.svar(it.id, "String")
                Datatype.DATO -> klageBehandling.svar(it.id, LocalDate.MIN)
                Datatype.FLERVALG -> klageBehandling.svar(it.id, listOf("String1", "String2"))
            }
        }
    }

    private fun KlageBehandling.finnEnBoolskOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.BOOLSK }.id
    }

    private fun KlageBehandling.finnEnTekstOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.TEKST }.id
    }

    private fun KlageBehandling.finnEnDatoOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.DATO }.id
    }

    private fun KlageBehandling.finnEnListeOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.FLERVALG }.id
    }
}
