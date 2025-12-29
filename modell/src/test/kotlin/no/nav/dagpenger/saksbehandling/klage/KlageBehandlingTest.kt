package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageBehandlingUtført
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.Behandles
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLING_UTFORT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.FERDIGSTILT
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
        val klageBehandling = KlageBehandling()

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
        val klageBehandling = KlageBehandling()
        klageBehandling.synligeOpplysninger().filter { opplysning ->
            opplysning.type in utfallOpplysningTyper &&
                opplysning.synlighet()
        } shouldBe emptySet()

        // Besvarer alle opplysninger som er synlige, unntatt formkrav
        klageBehandling
            .synligeOpplysninger()
            .filter { opplysning ->
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
        klageBehandling
            .synligeOpplysninger()
            .filter { opplysning ->
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
                opprettet = LocalDateTime.now(),
            )

        klageBehandling.tilstand().type shouldBe BEHANDLES

        val klageBehandlingUtført =
            KlageBehandlingUtført(
                behandlingId = klageBehandling.behandlingId,
                utførtAv = saksbehandler,
            )
        shouldThrow<IllegalStateException> {
            klageBehandling.behandlingUtført(
                behandlendeEnhet = "4408",
                hendelse = klageBehandlingUtført,
            )
        }

        klageBehandling.svar(synligOgPåkrevdOpplysning.opplysningId, Boolsk(false))
        klageBehandling.svar(utfallOpplysning.opplysningId, TekstVerdi("AVVIST"))

        shouldNotThrow<IllegalStateException> {
            klageBehandling.behandlingUtført(
                behandlendeEnhet = "4408",
                hendelse = klageBehandlingUtført,
            )
        }
        klageBehandling.tilstand().type shouldBe FERDIGSTILT
    }

    @Test
    fun `Opprettholdt klagebehandling får behandling utført og kan sendes til KA  når alle synlige og påkrevde opplysninger er utfylt`() {
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
        val behandlingId = UUIDv7.ny()
        val klageBehandling =
            KlageBehandling.rehydrer(
                behandlingId = behandlingId,
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
                tilstandslogg =
                    KlageTilstandslogg().also {
                        it.leggTil(
                            BEHANDLES,
                            hendelse =
                                KlageMottattHendelse(
                                    ident = "fnr",
                                    journalpostId = "jpid",
                                    sakId = UUID.randomUUID(),
                                    opprettet = LocalDateTime.now(),
                                ),
                        )
                    },
            )
        klageBehandling.tilstand().type shouldBe BEHANDLES

        val klageBehandlingUtført =
            KlageBehandlingUtført(
                behandlingId = klageBehandling.behandlingId,
                utførtAv = saksbehandler,
            )
        shouldThrow<IllegalStateException> {
            klageBehandling.behandlingUtført(
                behandlendeEnhet = "4408",
                hendelse = klageBehandlingUtført,
            )
        }

        klageBehandling.svar(synligOgPåkrevdOpplysning.opplysningId, Boolsk(false))
        klageBehandling.svar(utfallOpplysning.opplysningId, TekstVerdi("OPPRETTHOLDELSE"))

        shouldNotThrow<IllegalStateException> {
            klageBehandling.behandlingUtført(
                behandlendeEnhet = "4408",
                hendelse = klageBehandlingUtført,
            )
        }
        klageBehandling.tilstand().type shouldBe BEHANDLING_UTFORT

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                utsendingId = UUID.randomUUID(),
                ident = "fnr",
                journalpostId = "journalpostId",
                distribusjonId = "distribusjonId",
            )
        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "fagsakId",
                finnJournalpostIdForBehandling = { "" },
            )

        aksjon.shouldBeInstanceOf<KlageAksjon.OversendKlageinstans>()
    }

    @Test
    fun `Klagebehandling skal kunne avbrytes fra tilstand BEHANDLES`() {
        val klageBehandling = KlageBehandling()
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
        val klageBehandling = KlageBehandling()
        svarPåAlleOpplysninger(klageBehandling)
        val klageBehandlingUtført =
            KlageBehandlingUtført(
                behandlingId = klageBehandling.behandlingId,
                utførtAv = saksbehandler,
            )
        klageBehandling.behandlingUtført(
            behandlendeEnhet = "4408",
            hendelse = klageBehandlingUtført,
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

    private fun KlageBehandling.finnEnBoolskOpplysningId(): UUID =
        this
            .synligeOpplysninger()
            .first { opplysning -> opplysning.type.datatype == Datatype.BOOLSK }
            .opplysningId

    private fun KlageBehandling.finnEnTekstOpplysningId(): UUID =
        this
            .synligeOpplysninger()
            .first { opplysning -> opplysning.type.datatype == Datatype.TEKST }
            .opplysningId

    private fun KlageBehandling.finnEnDatoOpplysningId(): UUID =
        this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.DATO }.opplysningId

    private fun KlageBehandling.finnEnListeOpplysningId(): UUID =
        this
            .synligeOpplysninger()
            .first { opplysning -> opplysning.type.datatype == Datatype.FLERVALG }
            .opplysningId
}
