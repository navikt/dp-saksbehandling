package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import java.util.UUID

data class KlageBehandling(
    val behandlingId: UUID = UUIDv7.ny(),
    private val opplysninger: Set<Opplysning> = OpplysningBygger.lagOpplysninger(OpplysningType.entries.toSet()),
    private var tilstand: KlageTilstand = Behandles,
    private val journalpostId: String? = null,
    private var behandlendeEnhet: String? = null,
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
    init {
        steg.forEach { it.evaluerSynlighet(opplysninger) }
    }

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
            .verdi()
            .let {
                when (it) {
                    is Verdi.TekstVerdi -> UtfallType.valueOf(it.value)
                    is Verdi.TomVerdi -> null
                    else -> throw IllegalStateException("Utfall er av feil type: $it")
                }
            }
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
                klageBehandling.tilstand = OversendKlageinstans
            } else {
                klageBehandling.tilstand = Ferdigstilt
            }
        }

        override fun avbryt(
            klageBehandling: KlageBehandling,
            hendelse: AvbruttHendelse,
        ) {
            klageBehandling.tilstand = Avbrutt
        }
    }

    object OversendKlageinstans : KlageTilstand {
        override val type: Type = OVERSEND_KLAGEINSTANS

        override fun oversendtKlageinstans(
            klageBehandling: KlageBehandling,
            hendelse: OversendtKlageinstansHendelse,
        ) {
            klageBehandling.tilstand = Ferdigstilt
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
