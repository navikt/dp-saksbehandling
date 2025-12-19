package no.nav.dagpenger.saksbehandling.klage

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageBehandlingUtført
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import no.nav.dagpenger.saksbehandling.klage.UtfallType.Companion.toUtfallType
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

@ConsistentCopyVisibility
data class KlageBehandling private constructor(
    val behandlingId: UUID = UUIDv7.ny(),
    val opprettet: LocalDateTime,
    private val opplysninger: Set<Opplysning> = OpplysningBygger.lagOpplysninger(OpplysningType.entries.toSet()),
    private var tilstand: KlageTilstand = Behandles,
    private val journalpostId: String? = null,
    private var behandlendeEnhet: String? = null,
    private val _tilstandslogg: KlageTilstandslogg = KlageTilstandslogg(),
    private val steg: List<Steg> =
        listOf(
            KlagenGjelderSteg,
            FristvurderingSteg,
            FormkravSteg,
            VurderUtfallSteg,
            OversendKlageinstansSteg,
            FullmektigSteg,
        ),
) {
    constructor(
        journalpostId: String? = null,
        tilstandslogg: KlageTilstandslogg = KlageTilstandslogg(),
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) : this(
        journalpostId = journalpostId,
        _tilstandslogg = tilstandslogg,
        opprettet = opprettet,
    )

    init {
        steg.forEach { it.evaluerSynlighet(opplysninger) }
    }

    companion object {
        fun rehydrer(
            behandlingId: UUID,
            opprettet: LocalDateTime,
            opplysninger: Set<Opplysning> = OpplysningBygger.lagOpplysninger(OpplysningType.entries.toSet()),
            tilstand: KlageTilstand,
            journalpostId: String?,
            behandlendeEnhet: String?,
            tilstandslogg: KlageTilstandslogg = KlageTilstandslogg(),
            steg: List<Steg> =
                listOf(
                    KlagenGjelderSteg,
                    FristvurderingSteg,
                    FormkravSteg,
                    VurderUtfallSteg,
                    OversendKlageinstansSteg,
                    FullmektigSteg,
                ),
        ): KlageBehandling =
            KlageBehandling(
                behandlingId = behandlingId,
                opprettet = opprettet,
                opplysninger = opplysninger,
                tilstand = tilstand,
                journalpostId = journalpostId,
                behandlendeEnhet = behandlendeEnhet,
                _tilstandslogg = tilstandslogg,
                steg = steg,
            )
    }

    val tilstandslogg: KlageTilstandslogg
        get() = _tilstandslogg

    fun tilstand(): KlageTilstand = tilstand

    fun journalpostId(): String? = journalpostId

    fun behandlendeEnhet(): String? = behandlendeEnhet

    fun utfall(): UtfallType? =
        opplysninger
            .single { it.type == UTFALL }
            .verdi()
            .toUtfallType()

    fun hentOpplysning(opplysningId: UUID): Opplysning =
        opplysninger.singleOrNull { it.opplysningId == opplysningId }
            ?: throw IllegalArgumentException("Fant ikke opplysning med id $opplysningId")

    fun alleOpplysninger(): Set<Opplysning> = opplysninger

    fun synligeOpplysninger(): Set<Opplysning> = opplysninger.filter { it.synlighet() }.toSet()

    fun personIdent(): String =
        runCatching {
            _tilstandslogg
                .firstOrNull { it.hendelse is KlageMottattHendelse || it.hendelse is ManuellKlageMottattHendelse }
                ?.let {
                    if (it.hendelse is KlageMottattHendelse) {
                        (it.hendelse).ident
                    } else {
                        (it.hendelse as ManuellKlageMottattHendelse).ident
                    }
                }
        }.onFailure { e -> logger.error(e) { "Feil ved henting av personident for klagebehandling: ${this.behandlingId}" } }
            .getOrThrow()!!

    fun svar(
        opplysningId: UUID,
        verdi: Verdi,
    ) {
        hentOpplysning(opplysningId).also { opplysning ->
            opplysning.svar(verdi)
            evaluerSynlighetOgUtfall()
        }
    }

    private fun endreTilstand(
        nyTilstand: KlageTilstand,
        hendelse: Hendelse,
    ) {
        logger.info {
            "Endrer klagetilstand fra ${this.tilstand.type} til ${nyTilstand.type} for klage ${this.behandlingId} " +
                "basert på hendelse: ${hendelse.javaClass.simpleName} "
        }
        this.tilstand = nyTilstand
        this._tilstandslogg.leggTil(nyTilstand.type, hendelse)
    }

    private fun evaluerSynlighetOgUtfall() {
        this.steg.forEach { steg ->
            steg.evaluerSynlighet(opplysninger)
        }
    }

    private fun kanFerdigstilles(): Boolean {
        val tommeOpplysninger =
            opplysninger.filter {
                it.synlighet() && it.type.påkrevd && it.verdi() == Verdi.TomVerdi
            }
        return tommeOpplysninger.isEmpty()
    }

    fun behandlingUtført(
        behandlendeEnhet: String,
        hendelse: KlageBehandlingUtført,
    ) {
        if (!this.kanFerdigstilles()) {
            throw IllegalStateException("Kan ikke ferdigstille klagebehandling når påkrevde opplysninger ikke er utfylt")
        }
        this.behandlendeEnhet = behandlendeEnhet
        tilstand.behandlingUtført(
            klageBehandling = this,
            hendelse = hendelse,
        )
    }

    fun oversendtTilKlageinstans(hendelse: OversendtKlageinstansHendelse) {
        tilstand.oversendtKlageinstans(
            klageBehandling = this,
            hendelse = hendelse,
        )
    }

    fun avbryt(hendelse: AvbruttHendelse) {
        tilstand.avbryt(
            klageBehandling = this,
            hendelse = hendelse,
        )
    }

    fun vedtakDistribuert(hendelse: UtsendingDistribuert) {
        tilstand.vedtakDistribuert(
            klageBehandling = this,
            hendelse = hendelse,
        )
    }

    object Behandles : KlageTilstand {
        override val type: Type = BEHANDLES

        override fun behandlingUtført(
            klageBehandling: KlageBehandling,
            hendelse: KlageBehandlingUtført,
        ) {
            val utfall =
                requireNotNull(klageBehandling.utfall()) {
                    "Utfall må være satt for å ferdigstille klagebehandling"
                }
            when (utfall) {
                UtfallType.OPPRETTHOLDELSE -> {
                    klageBehandling.endreTilstand(
                        nyTilstand = BehandlingUtført,
                        hendelse = hendelse,
                    )
                }

                UtfallType.MEDHOLD -> {
                    throw IllegalStateException("Kan ikke ferdigstille klager med medhold eller delvis medhold (enda).")
                }

                UtfallType.DELVIS_MEDHOLD -> {
                    throw IllegalStateException("Kan ikke ferdigstille klager med medhold eller delvis medhold (enda).")
                }

                UtfallType.AVVIST -> TODO()
            }

//            if (klageBehandling.utfall() == UtfallType.OPPRETTHOLDELSE) {
//                klageBehandling.endreTilstand(
//                    nyTilstand = OversendKlageinstans,
//                    hendelse = hendelse,
//                )
//            } else if (klageBehandling.utfall() in setOf(UtfallType.DELVIS_MEDHOLD, UtfallType.MEDHOLD)) {
//                // TODO: implementer ferdigstilling av disse utfallene
//                throw IllegalStateException("Kan ikke ferdigstille klager med medhold eller delvis medhold (enda).")
//            } else {
//                klageBehandling.endreTilstand(
//                    nyTilstand = Ferdigstilt,
//                    hendelse = hendelse,
//                )
//            }
        }

        override fun avbryt(
            klageBehandling: KlageBehandling,
            hendelse: AvbruttHendelse,
        ) {
            klageBehandling.endreTilstand(
                nyTilstand = Avbrutt,
                hendelse = hendelse,
            )
        }
    }

    object OversendKlageinstans : KlageTilstand {
        override val type: Type = OVERSEND_KLAGEINSTANS

        override fun oversendtKlageinstans(
            klageBehandling: KlageBehandling,
            hendelse: OversendtKlageinstansHendelse,
        ) {
            klageBehandling.endreTilstand(
                nyTilstand = Ferdigstilt,
                hendelse = hendelse,
            )
        }
    }

    object BehandlingUtført : KlageTilstand {
        override val type: Type = Type.BEHANDLING_UTFORT

        override fun vedtakDistribuert(
            klageBehandling: KlageBehandling,
            hendelse: UtsendingDistribuert,
        ) {
            if (klageBehandling.utfall() == UtfallType.OPPRETTHOLDELSE) {
                klageBehandling.endreTilstand(
                    nyTilstand = OversendKlageinstans,
                    hendelse = hendelse,
                )
            } else {
                klageBehandling.endreTilstand(
                    nyTilstand = Ferdigstilt,
                    hendelse = hendelse,
                )
            }
        }
    }

    object Ferdigstilt : KlageTilstand {
        override val type: Type = FERDIGSTILT
    }

    object Avbrutt : KlageTilstand {
        override val type: Type = AVBRUTT
    }

    sealed interface KlageTilstand {
        val type: Type

        fun behandlingUtført(
            klageBehandling: KlageBehandling,
            hendelse: KlageBehandlingUtført,
        ): Unit = throw IllegalStateException("Kan ikke ferdigstille klagebehandling i tilstand $type")

        fun avbryt(
            klageBehandling: KlageBehandling,
            hendelse: AvbruttHendelse,
        ): Unit = throw IllegalStateException("Kan ikke avbryte klagebehandling i tilstand $type")

        fun oversendtKlageinstans(
            klageBehandling: KlageBehandling,
            hendelse: OversendtKlageinstansHendelse,
        ): Unit = throw IllegalStateException("Kan ikke motta hendelse om oversendt klageinstans i tilstand $type")

        fun vedtakDistribuert(
            klageBehandling: KlageBehandling,
            hendelse: UtsendingDistribuert,
        ): Unit = throw IllegalStateException("Kan ikke motta hendelse om oversendt klageinstans i tilstand $type")

        enum class Type {
            BEHANDLES,
            BEHANDLING_UTFORT,
            OVERSEND_KLAGEINSTANS,
            FERDIGSTILT,
            AVBRUTT,
        }
    }
}
