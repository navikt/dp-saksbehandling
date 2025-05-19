package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.Type.OVERSEND_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.formkravOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.klagenGjelderOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger.utfallOpplysningTyper
import no.nav.dagpenger.saksbehandling.klage.Verdi.Boolsk
import no.nav.dagpenger.saksbehandling.klage.Verdi.Dato
import no.nav.dagpenger.saksbehandling.klage.Verdi.Flervalg
import no.nav.dagpenger.saksbehandling.klage.Verdi.TekstVerdi
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

        klageBehandling.svar(boolskOpplysningId, Boolsk(false))
        klageBehandling.hentOpplysning(boolskOpplysningId).verdi().let {
            require(it is Boolsk)
            it.value shouldBe false
        }
        klageBehandling.svar(boolskOpplysningId, Boolsk(true))
        klageBehandling.hentOpplysning(boolskOpplysningId).verdi().let {
            require(it is Boolsk)
            it.value shouldBe true
        }

        klageBehandling.svar(stringOpplysningId, TekstVerdi("String"))
        klageBehandling.hentOpplysning(stringOpplysningId).verdi().let {
            require(it is TekstVerdi)
            it.value shouldBe "String"
        }

        klageBehandling.svar(datoOpplysningId, Dato(LocalDate.MIN))
        klageBehandling.hentOpplysning(datoOpplysningId).verdi().let {
            require(it is Dato)
            it.value shouldBe LocalDate.MIN
        }

        val valg = klageBehandling.hentOpplysning(listeOpplysningId).type.valgmuligheter
        klageBehandling.svar(listeOpplysningId, Flervalg(valg))
        klageBehandling.hentOpplysning(listeOpplysningId).verdi().let {
            require(it is Flervalg)
            it.value shouldBe valg
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
                Datatype.BOOLSK -> klageBehandling.svar(it.opplysningId, Boolsk(true))
                Datatype.TEKST -> klageBehandling.svar(it.opplysningId, TekstVerdi("String"))
                Datatype.DATO -> klageBehandling.svar(it.opplysningId, Dato(LocalDate.MIN))
                Datatype.FLERVALG -> klageBehandling.svar(it.opplysningId, Flervalg(it.valgmuligheter))
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
            klageBehandling.svar(it.opplysningId, Boolsk(true))
        }

        klageBehandling.synligeOpplysninger().filter { opplysning ->
            opplysning.type in utfallOpplysningTyper &&
                opplysning.synlighet()
        } shouldNotBe emptySet<Opplysning>()
    }

    @Test
    fun `Avvist klagebehandling får tilstand ferdigstilt når alle synlige og påkrevde opplysninger er utfylt`() {
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
        val utfallOpplysning =
            Opplysning(
                type = OpplysningType.UTFALL,
                verdi = Verdi.TomVerdi,
                synlig = true,
            )
        val klageBehandling =
            KlageBehandling(
                steg = emptyList(),
                opplysninger = setOf(synligOgPåkrevdOpplysning, ikkePåkrevdOpplysning, ikkeSynligOpplysning, utfallOpplysning),
            )
        klageBehandling.tilstand() shouldBe BEHANDLES

        shouldThrow<IllegalStateException> { klageBehandling.saksbehandlingFerdig("4408") }

        klageBehandling.svar(synligOgPåkrevdOpplysning.opplysningId, Boolsk(false))
        klageBehandling.svar(utfallOpplysning.opplysningId, TekstVerdi("AVVIST"))

        shouldNotThrow<IllegalStateException> { klageBehandling.saksbehandlingFerdig("4408") }
        klageBehandling.tilstand() shouldBe FERDIGSTILT
    }

    @Test
    fun `Opprettholdt klagebehandling får tilstand oversend KA når alle synlige og påkrevde opplysninger er utfylt`() {
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
        val utfallOpplysning =
            Opplysning(
                type = OpplysningType.UTFALL,
                verdi = Verdi.TomVerdi,
                synlig = true,
            )
        val klageBehandling =
            KlageBehandling(
                steg = emptyList(),
                opplysninger = setOf(synligOgPåkrevdOpplysning, ikkePåkrevdOpplysning, ikkeSynligOpplysning, utfallOpplysning),
            )
        klageBehandling.tilstand() shouldBe BEHANDLES

        shouldThrow<IllegalStateException> { klageBehandling.saksbehandlingFerdig("4408") }

        klageBehandling.svar(synligOgPåkrevdOpplysning.opplysningId, Boolsk(false))
        klageBehandling.svar(utfallOpplysning.opplysningId, TekstVerdi("OPPRETTHOLDELSE"))

        shouldNotThrow<IllegalStateException> { klageBehandling.saksbehandlingFerdig("4408") }
        klageBehandling.tilstand() shouldBe OVERSEND_KLAGEINSTANS
    }

    @Test
    fun `Klagebehandling skal kunne avbrytes fra tilstand BEHANDLES`() {
        val klageBehandling = KlageBehandling()
        klageBehandling.tilstand() shouldBe BEHANDLES

        klageBehandling.avbryt()
        klageBehandling.tilstand() shouldBe AVBRUTT
    }

    @Test
    fun `Klagebehandling skal ikke kunne avbrytes fra tilstand FERDIGSTILT eller OVERSEND_KLAGEINSTANS`() {
        val klageBehandling = KlageBehandling()
        svarPåAlleOpplysninger(klageBehandling)
        klageBehandling.saksbehandlingFerdig("4408")

        shouldThrow<IllegalStateException> { klageBehandling.avbryt() }
    }

    private fun svarPåAlleOpplysninger(klageBehandling: KlageBehandling) {
        klageBehandling.alleOpplysninger().forEach {
            when (it.type.datatype) {
                Datatype.BOOLSK -> klageBehandling.svar(it.opplysningId, Boolsk(true))
                Datatype.TEKST -> klageBehandling.svar(it.opplysningId, TekstVerdi(it.valgmuligheter.firstOrNull() ?: "String"))
                Datatype.DATO -> klageBehandling.svar(it.opplysningId, Dato(LocalDate.MIN))
                Datatype.FLERVALG -> klageBehandling.svar(it.opplysningId, Flervalg(it.valgmuligheter))
            }
        }
    }

    private fun KlageBehandling.finnEnBoolskOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.BOOLSK }.opplysningId
    }

    private fun KlageBehandling.finnEnTekstOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.TEKST }.opplysningId
    }

    private fun KlageBehandling.finnEnDatoOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.DATO }.opplysningId
    }

    private fun KlageBehandling.finnEnListeOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.FLERVALG }.opplysningId
    }
}
