package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.Behandles
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
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
    private val person =
        Person(
            ident = "12345678910",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )

    @Test
    fun `Skal kunne svare og endre på opplysninger med ulike datatyper`() {
        val klageBehandling = KlageBehandling(person = person)

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
        val klageBehandling = KlageBehandling(person = person)
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
            KlageBehandling.rehydrer(
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
                person = person,
                opprettet = LocalDateTime.now(),
            )

        klageBehandling.tilstand().type shouldBe BEHANDLES

        val klageFerdigbehandletHendelse =
            KlageFerdigbehandletHendelse(
                behandlingId = klageBehandling.behandlingId,
                utførtAv = saksbehandler,
            )
        shouldThrow<IllegalStateException> {
            klageBehandling.saksbehandlingFerdig(
                behandlendeEnhet = "4408",
                hendelse = klageFerdigbehandletHendelse,
            )
        }

        klageBehandling.svar(synligOgPåkrevdOpplysning.opplysningId, Boolsk(false))
        klageBehandling.svar(utfallOpplysning.opplysningId, TekstVerdi("AVVIST"))

        shouldNotThrow<IllegalStateException> {
            klageBehandling.saksbehandlingFerdig(
                behandlendeEnhet = "4408",
                hendelse = klageFerdigbehandletHendelse,
            )
        }
        klageBehandling.tilstand().type shouldBe FERDIGSTILT
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
            KlageBehandling.rehydrer(
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
                person = person,
                opprettet = LocalDateTime.now(),
            )
        klageBehandling.tilstand().type shouldBe BEHANDLES

        val klageFerdigbehandletHendelse =
            KlageFerdigbehandletHendelse(
                behandlingId = klageBehandling.behandlingId,
                utførtAv = saksbehandler,
            )
        shouldThrow<IllegalStateException> {
            klageBehandling.saksbehandlingFerdig(
                behandlendeEnhet = "4408",
                hendelse = klageFerdigbehandletHendelse,
            )
        }

        klageBehandling.svar(synligOgPåkrevdOpplysning.opplysningId, Boolsk(false))
        klageBehandling.svar(utfallOpplysning.opplysningId, TekstVerdi("OPPRETTHOLDELSE"))

        shouldNotThrow<IllegalStateException> {
            klageBehandling.saksbehandlingFerdig(
                behandlendeEnhet = "4408",
                hendelse = klageFerdigbehandletHendelse,
            )
        }
        klageBehandling.tilstand().type shouldBe OVERSEND_KLAGEINSTANS
    }

    @Test
    fun `Klagebehandling skal kunne avbrytes fra tilstand BEHANDLES`() {
        val klageBehandling = KlageBehandling(person = person)
        klageBehandling.tilstand().type shouldBe BEHANDLES

        klageBehandling.avbryt(
            hendelse =
                AvbruttHendelse(
                    behandlingId = klageBehandling.behandlingId,
                    utførtAv = saksbehandler,
                ),
        )

        klageBehandling.tilstand().type shouldBe AVBRUTT
    }

    @Test
    fun `Klagebehandling skal ikke kunne avbrytes fra tilstand FERDIGSTILT eller OVERSEND_KLAGEINSTANS`() {
        val klageBehandling = KlageBehandling(person = person)
        svarPåAlleOpplysninger(klageBehandling)
        val klageFerdigbehandletHendelse =
            KlageFerdigbehandletHendelse(
                behandlingId = klageBehandling.behandlingId,
                utførtAv = saksbehandler,
            )
        klageBehandling.saksbehandlingFerdig(
            behandlendeEnhet = "4408",
            hendelse = klageFerdigbehandletHendelse,
        )

        klageBehandling.tilstand().type shouldBe FERDIGSTILT

        shouldThrow<IllegalStateException> {
            klageBehandling.avbryt(
                hendelse =
                    AvbruttHendelse(
                        behandlingId = klageBehandling.behandlingId,
                        utførtAv = saksbehandler,
                    ),
            )
        }
    }

    private fun svarPåAlleOpplysninger(klageBehandling: KlageBehandling) {
        klageBehandling.alleOpplysninger().forEach {
            when (it.type.datatype) {
                Datatype.BOOLSK -> klageBehandling.svar(it.opplysningId, Boolsk(true))
                Datatype.TEKST ->
                    klageBehandling.svar(
                        opplysningId = it.opplysningId,
                        verdi =
                            TekstVerdi(
                                value =
                                    when (it.type) {
                                        OpplysningType.UTFALL -> "Avvist"
                                        else -> it.valgmuligheter.firstOrNull() ?: "String"
                                    },
                            ),
                    )

                Datatype.DATO -> klageBehandling.svar(it.opplysningId, Dato(LocalDate.MIN))
                Datatype.FLERVALG -> klageBehandling.svar(it.opplysningId, Flervalg(it.valgmuligheter))
            }
        }
    }

    private fun KlageBehandling.finnEnBoolskOpplysningId(): UUID {
        return this.synligeOpplysninger()
            .first { opplysning -> opplysning.type.datatype == Datatype.BOOLSK }.opplysningId
    }

    private fun KlageBehandling.finnEnTekstOpplysningId(): UUID {
        return this.synligeOpplysninger()
            .first { opplysning -> opplysning.type.datatype == Datatype.TEKST }.opplysningId
    }

    private fun KlageBehandling.finnEnDatoOpplysningId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.DATO }.opplysningId
    }

    private fun KlageBehandling.finnEnListeOpplysningId(): UUID {
        return this.synligeOpplysninger()
            .first { opplysning -> opplysning.type.datatype == Datatype.FLERVALG }.opplysningId
    }
}
