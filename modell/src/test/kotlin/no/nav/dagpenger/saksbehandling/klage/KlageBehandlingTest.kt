package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.klage.Klage.Behandles
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand.Type.OVERSENDT_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
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
import java.time.LocalDateTime
import java.util.UUID

class KlageBehandlingTest {
    private val saksbehandler =
        Saksbehandler(
            navIdent = "saksbehandler",
            grupper = setOf("SaksbehandlerADGruppe"),
            tilganger = setOf(SAKSBEHANDLER),
        )

    @Test
    fun `Skal kunne svare og endre på opplysninger med ulike datatyper`() {
        val klage = Klage()

        val boolskOpplysningId = klage.finnEnBoolskOpplysningId()
        val stringOpplysningId = klage.finnEnTekstOpplysningId()
        val datoOpplysningId = klage.finnEnDatoOpplysningId()
        val listeOpplysningId = klage.finnEnListeOpplysningId()

        klage.svar(boolskOpplysningId, Boolsk(false))
        klage.hentOpplysning(boolskOpplysningId).verdi().let {
            require(it is Boolsk)
            it.value shouldBe false
        }
        klage.svar(boolskOpplysningId, Boolsk(true))
        klage.hentOpplysning(boolskOpplysningId).verdi().let {
            require(it is Boolsk)
            it.value shouldBe true
        }

        klage.svar(stringOpplysningId, TekstVerdi("String"))
        klage.hentOpplysning(stringOpplysningId).verdi().let {
            require(it is TekstVerdi)
            it.value shouldBe "String"
        }

        klage.svar(datoOpplysningId, Dato(LocalDate.MIN))
        klage.hentOpplysning(datoOpplysningId).verdi().let {
            require(it is Dato)
            it.value shouldBe LocalDate.MIN
        }

        val valg = klage.hentOpplysning(listeOpplysningId).type.valgmuligheter
        klage.svar(listeOpplysningId, Flervalg(valg))
        klage.hentOpplysning(listeOpplysningId).verdi().let {
            require(it is Flervalg)
            it.value shouldBe valg
        }
    }

    @Test
    fun `Utfall skal kunne velges når alle behandlingsopplysninger er utfylt`() {
        val klage = Klage()
        klage.synligeOpplysninger().filter { opplysning ->
            opplysning.type in utfallOpplysningTyper &&
                opplysning.synlighet()
        } shouldBe emptySet()

        // Besvarer alle opplysninger som er synlige, unntatt formkrav
        klage.synligeOpplysninger().filter { opplysning ->
            opplysning.type in klagenGjelderOpplysningTyper +
                fristvurderingOpplysningTyper +
                oversittetFristOpplysningTyper
        }.forEach {
            when (it.type.datatype) {
                Datatype.BOOLSK -> klage.svar(it.opplysningId, Boolsk(true))
                Datatype.TEKST -> klage.svar(it.opplysningId, TekstVerdi("String"))
                Datatype.DATO -> klage.svar(it.opplysningId, Dato(LocalDate.MIN))
                Datatype.FLERVALG -> klage.svar(it.opplysningId, Flervalg(it.valgmuligheter))
            }
        }

        klage.synligeOpplysninger().filter { opplysning ->
            opplysning.type in utfallOpplysningTyper &&
                opplysning.synlighet()
        } shouldBe emptySet()

        // Besvarer formkrav
        klage.synligeOpplysninger().filter { opplysning ->
            opplysning.type in formkravOpplysningTyper
        }.forEach {
            klage.svar(it.opplysningId, Boolsk(true))
        }

        klage.synligeOpplysninger().filter { opplysning ->
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
        val klage =
            Klage.rehydrer(
                behandlingId = UUIDv7.ny(),
                opplysninger =
                    setOf(
                        synligOgPåkrevdOpplysning,
                        ikkePåkrevdOpplysning,
                        ikkeSynligOpplysning,
                        utfallOpplysning,
                    ),
                tilstand = Behandles,
                journalpostId = null,
                behandlendeEnhet = null,
                opprettet = LocalDateTime.now(),
            )

        klage.tilstand().type shouldBe BEHANDLES

        val klageFerdigbehandletHendelse =
            KlageFerdigbehandletHendelse(
                behandlingId = klage.behandlingId,
                utførtAv = saksbehandler,
            )
        shouldThrow<IllegalStateException> {
            klage.saksbehandlingFerdig(
                behandlendeEnhet = "4408",
                hendelse = klageFerdigbehandletHendelse,
            )
        }

        klage.svar(synligOgPåkrevdOpplysning.opplysningId, Boolsk(false))
        klage.svar(utfallOpplysning.opplysningId, TekstVerdi("AVVIST"))

        shouldNotThrow<IllegalStateException> {
            klage.saksbehandlingFerdig(
                behandlendeEnhet = "4408",
                hendelse = klageFerdigbehandletHendelse,
            )
        }
        klage.tilstand().type shouldBe FERDIGSTILT
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
        val klage =
            Klage.rehydrer(
                behandlingId = UUIDv7.ny(),
                opplysninger =
                    setOf(
                        synligOgPåkrevdOpplysning,
                        ikkePåkrevdOpplysning,
                        ikkeSynligOpplysning,
                        utfallOpplysning,
                    ),
                tilstand = Behandles,
                journalpostId = null,
                behandlendeEnhet = null,
                opprettet = LocalDateTime.now(),
            )
        klage.tilstand().type shouldBe BEHANDLES

        val klageFerdigbehandletHendelse =
            KlageFerdigbehandletHendelse(
                behandlingId = klage.behandlingId,
                utførtAv = saksbehandler,
            )
        shouldThrow<IllegalStateException> {
            klage.saksbehandlingFerdig(
                behandlendeEnhet = "4408",
                hendelse = klageFerdigbehandletHendelse,
            )
        }

        klage.svar(synligOgPåkrevdOpplysning.opplysningId, Boolsk(false))
        klage.svar(utfallOpplysning.opplysningId, TekstVerdi("OPPRETTHOLDELSE"))

        shouldNotThrow<IllegalStateException> {
            klage.saksbehandlingFerdig(
                behandlendeEnhet = "4408",
                hendelse = klageFerdigbehandletHendelse,
            )
        }
        klage.tilstand().type shouldBe OVERSEND_KLAGEINSTANS
    }

    @Test
    fun `Klagebehandling skal kunne avbrytes fra tilstand BEHANDLES`() {
        val klage = Klage()
        klage.tilstand().type shouldBe BEHANDLES

        klage.avbryt(
            hendelse =
                AvbruttHendelse(
                    behandlingId = klage.behandlingId,
                    utførtAv = saksbehandler,
                ),
        )

        klage.tilstand().type shouldBe AVBRUTT
    }

    @Test
    fun `Klagebehandling skal ikke kunne avbrytes fra tilstand FERDIGSTILT`() {
        val klage = Klage()
        svarPåAlleOpplysninger(klage)
        val klageFerdigbehandletHendelse =
            KlageFerdigbehandletHendelse(
                behandlingId = klage.behandlingId,
                utførtAv = saksbehandler,
            )
        klage.saksbehandlingFerdig(
            behandlendeEnhet = "4408",
            hendelse = klageFerdigbehandletHendelse,
        )

        klage.tilstand().type shouldBe FERDIGSTILT

        shouldThrow<IllegalStateException> {
            klage.avbryt(
                hendelse =
                    AvbruttHendelse(
                        behandlingId = klage.behandlingId,
                        utførtAv = saksbehandler,
                    ),
            )
        }
    }

    @Test
    fun `Klagebehandling skal ikke kunne avbrytes fra tilstand OVERSEND_KLAGEINSTANS`() {
        val klage = Klage()
        svarPåAlleOpplysninger(klage = klage, utfall = UtfallType.OPPRETTHOLDELSE)
        val klageFerdigbehandletHendelse =
            KlageFerdigbehandletHendelse(
                behandlingId = klage.behandlingId,
                utførtAv = saksbehandler,
            )
        klage.saksbehandlingFerdig(
            behandlendeEnhet = "4408",
            hendelse = klageFerdigbehandletHendelse,
        )

        klage.tilstand().type shouldBe OVERSEND_KLAGEINSTANS

        shouldThrow<IllegalStateException> {
            klage.avbryt(
                hendelse =
                    AvbruttHendelse(
                        behandlingId = klage.behandlingId,
                        utførtAv = saksbehandler,
                    ),
            )
        }
    }

    @Test
    fun `Klagebehandling skal ikke kunne avbrytes fra tilstand OVERSENDT_KLAGEINSTANS`() {
        val klage = Klage()
        svarPåAlleOpplysninger(klage = klage, utfall = UtfallType.OPPRETTHOLDELSE)
        val klageFerdigbehandletHendelse =
            KlageFerdigbehandletHendelse(
                behandlingId = klage.behandlingId,
                utførtAv = saksbehandler,
            )
        klage.saksbehandlingFerdig(
            behandlendeEnhet = "4408",
            hendelse = klageFerdigbehandletHendelse,
        )

        klage.tilstand().type shouldBe OVERSEND_KLAGEINSTANS

        klage.oversendtTilKlageinstans(
            hendelse = OversendtKlageinstansHendelse(behandlingId = klage.behandlingId),
        )

        klage.tilstand().type shouldBe OVERSENDT_KLAGEINSTANS

        shouldThrow<IllegalStateException> {
            klage.avbryt(
                hendelse =
                    AvbruttHendelse(
                        behandlingId = klage.behandlingId,
                        utførtAv = saksbehandler,
                    ),
            )
        }
    }

    private fun svarPåAlleOpplysninger(
        klage: Klage,
        utfall: UtfallType = UtfallType.AVVIST,
    ) {
        klage.alleOpplysninger().forEach {
            when (it.type.datatype) {
                Datatype.BOOLSK -> klage.svar(it.opplysningId, Boolsk(true))
                Datatype.TEKST ->
                    klage.svar(
                        opplysningId = it.opplysningId,
                        verdi =
                            TekstVerdi(
                                value =
                                    when (it.type) {
                                        OpplysningType.UTFALL -> utfall.tekst
                                        else -> it.valgmuligheter.firstOrNull() ?: "String"
                                    },
                            ),
                    )

                Datatype.DATO -> klage.svar(it.opplysningId, Dato(LocalDate.MIN))
                Datatype.FLERVALG -> klage.svar(it.opplysningId, Flervalg(it.valgmuligheter))
            }
        }
    }

    private fun Klage.finnEnBoolskOpplysningId(): UUID {
        return this.synligeOpplysninger()
            .first { opplysning -> opplysning.type.datatype == Datatype.BOOLSK }.opplysningId
    }

    private fun Klage.finnEnTekstOpplysningId(): UUID {
        return this.synligeOpplysninger()
            .first { opplysning -> opplysning.type.datatype == Datatype.TEKST }.opplysningId
    }

    private fun Klage.finnEnDatoOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.DATO }.opplysningId
    }

    private fun Klage.finnEnListeOpplysningId(): UUID {
        return this.synligeOpplysninger()
            .first { opplysning -> opplysning.type.datatype == Datatype.FLERVALG }.opplysningId
    }
}
