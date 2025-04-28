package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.BehandlingTilstand
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.BehandlingTilstand.KLAR_TIL_BEHANDLING
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

    @Test
    fun `En klagebehandling er ferdigstilt når alle synlige og påkrevde opplysninger er utfylt`() {
        val opplysning1 =
            Opplysning(
                type = OpplysningType.OPPREISNING_OVERSITTET_FRIST,
                verdi = Verdi.TomVerdi,
                synlig = true,
            )

        val opplysning2 =
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
        KlageBehandling(
            steg = emptyList(),
            opplysninger = setOf(opplysning1, opplysning2, ikkePåkrevdOpplysning, ikkeSynligOpplysning),
        ).let { klageBehandling ->
            klageBehandling.kanFerdigstilles() shouldBe false

            klageBehandling.svar(opplysning1.id, false)
            klageBehandling.svar(opplysning2.id, false)

            klageBehandling.kanFerdigstilles() shouldBe true
        }
    }

    @Test
    fun `Skal kunne opprette en klage i tilstanden KLAR_TIL_BEHANDLING`() {
        val klageBehandling =
            KlageBehandling(
                behandlingId = UUID.randomUUID(),
            )
        klageBehandling.hentTilstand() shouldBe KLAR_TIL_BEHANDLING
    }

    @Test
    fun `Når klagebehandlingen har fylt ut alle nødvendige opplysninger skal tilsanden kunne settes til ferdigstilt`() {
        val klageBehandling =
            KlageBehandling(
                behandlingId = UUID.randomUUID(),
            )
        klageBehandling.hentTilstand() shouldBe KLAR_TIL_BEHANDLING

        // Besvarer alle opplysninger som er synlige, unntatt formkrav
        svarPåAlleOpplysninger(klageBehandling)
        // ender bare opp med å ha 11 synlige opplysninger så her er det noe litt funky
        // TODO: lag en ordentlig livssyklustest der alle opplysningene svares på eksplisitt.
        klageBehandling.hentTilstand() shouldNotBe BehandlingTilstand.FERDIGSTILT
        klageBehandling.kanFerdigstilles() shouldBe true
        klageBehandling.ferdigstill()
        klageBehandling.hentTilstand() shouldBe BehandlingTilstand.FERDIGSTILT
    }

    @Test
    fun `klagen skal kunne avbrytes fra tilstand klar_til_behandling`() {
        val klageBehandling =
            KlageBehandling(
                behandlingId = UUID.randomUUID(),
            )
        klageBehandling.hentTilstand() shouldBe KLAR_TIL_BEHANDLING

        klageBehandling.avbryt()
        klageBehandling.hentTilstand() shouldBe BehandlingTilstand.AVBRUTT
    }

    @Test
    fun `klagen skal ikke kunne avbrytes fra tilstand ferdigstilt`() {
        val klageBehandling =
            KlageBehandling(
                behandlingId = UUID.randomUUID(),
            )
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
