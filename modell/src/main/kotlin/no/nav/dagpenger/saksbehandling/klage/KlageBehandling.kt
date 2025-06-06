package no.nav.dagpenger.saksbehandling.klage

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
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

data class KlageBehandling private constructor(
    val behandlingId: UUID = UUIDv7.ny(),
    val person: Person,
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
        person: Person,
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) : this (
        journalpostId = journalpostId,
        _tilstandslogg = tilstandslogg,
        person = person,
        opprettet = opprettet,
    )

    init {
        steg.forEach { it.evaluerSynlighet(opplysninger) }
    }

    companion object {
        fun rehydrer(
            behandlingId: UUID,
            person: Person,
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
                person = person,
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

    fun tilstand(): KlageTilstand {
        return tilstand
    }

    fun journalpostId(): String? {
        return journalpostId
    }

    fun behandlendeEnhet(): String? {
        return behandlendeEnhet
    }

    fun utfall(): UtfallType? {
        return opplysninger
            .single { it.type == UTFALL }
            .verdi().toUtfallType()
    }

    fun hentOpplysning(opplysningId: UUID): Opplysning {
        return opplysninger.singleOrNull { it.opplysningId == opplysningId }
            ?: throw IllegalArgumentException("Fant ikke opplysning med id $opplysningId")
    }

    fun alleOpplysninger(): Set<Opplysning> {
        return opplysninger
    }

    fun synligeOpplysninger(): Set<Opplysning> {
        return opplysninger.filter { it.synlighet() }.toSet()
    }

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

    fun saksbehandlingFerdig(
        behandlendeEnhet: String,
        hendelse: KlageFerdigbehandletHendelse,
    ) {
        if (!this.kanFerdigstilles()) {
            throw IllegalStateException("Kan ikke ferdigstille klagebehandling når påkrevde opplysninger ikke er utfylt")
        }
        this.behandlendeEnhet = behandlendeEnhet
        tilstand.saksbehandlingFerdig(
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

    object Behandles : KlageTilstand {
        override val type: Type = BEHANDLES

        override fun saksbehandlingFerdig(
            klageBehandling: KlageBehandling,
            hendelse: KlageFerdigbehandletHendelse,
        ) {
            if (klageBehandling.utfall() == UtfallType.OPPRETTHOLDELSE) {
                klageBehandling.endreTilstand(
                    nyTilstand = OversendKlageinstans,
                    hendelse = hendelse,
                )
            } else if (klageBehandling.utfall() in setOf(UtfallType.DELVIS_MEDHOLD, UtfallType.MEDHOLD)) {
                // TODO: implementer ferdigstilling av disse utfallene
                throw IllegalStateException("Kan ikke ferdigstille klager med medhold eller delvis medhold (enda).")
            } else {
                klageBehandling.endreTilstand(
                    nyTilstand = Ferdigstilt,
                    hendelse = hendelse,
                )
            }
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

    object Ferdigstilt : KlageTilstand {
        override val type: Type = FERDIGSTILT
    }

    object Avbrutt : KlageTilstand {
        override val type: Type = AVBRUTT
    }

    sealed interface KlageTilstand {
        val type: Type

        fun saksbehandlingFerdig(
            klageBehandling: KlageBehandling,
            hendelse: KlageFerdigbehandletHendelse,
        ) {
            throw IllegalStateException("Kan ikke ferdigstille klagebehandling i tilstand $type")
        }

        fun avbryt(
            klageBehandling: KlageBehandling,
            hendelse: AvbruttHendelse,
        ) {
            throw IllegalStateException("Kan ikke avbryte klagebehandling i tilstand $type")
        }

        fun oversendtKlageinstans(
            klageBehandling: KlageBehandling,
            hendelse: OversendtKlageinstansHendelse,
        ) {
            throw IllegalStateException("Kan ikke motta hendelse om oversendt klageinstans i tilstand $type")
        }

        enum class Type {
            BEHANDLES,
            OVERSEND_KLAGEINSTANS,
            FERDIGSTILT,
            AVBRUTT,
        }
    }
}
