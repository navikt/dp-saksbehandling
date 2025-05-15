package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import java.util.UUID

data class KlageBehandling(
    val behandlingId: UUID = UUIDv7.ny(),
    private val opplysninger: Set<Opplysning> = OpplysningBygger.lagOpplysninger(OpplysningType.entries.toSet()),
    private var tilstand: BehandlingTilstand = BehandlingTilstand.BEHANDLES,
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

    fun alleOpplysninger(): Set<Opplysning> {
        return opplysninger
    }

    fun synligeOpplysninger(): Set<Opplysning> {
        return opplysninger.filter { it.synlighet() }.toSet()
    }

    fun saksbehandlingFerdig(enhetsnummer: String) {
        if (this.tilstand == BehandlingTilstand.AVBRUTT) {
            throw IllegalStateException("Kan ikke ferdigstille klagebehandling når den er avbrutt")
        }
        if (!kanFerdigstilles()) {
            throw IllegalStateException("Kan ikke ferdigstille klagebehandling når påkrevde opplysninger ikke er utfylt")
        }
        this.behandlendeEnhet = enhetsnummer
        if (this.utfall() == UtfallType.OPPRETTHOLDELSE) {
            this.tilstand = BehandlingTilstand.OVERSEND_KLAGEINSTANS
        } else {
            this.tilstand = BehandlingTilstand.FERDIGSTILT
        }
    }

    fun oversendtTilKlageinstans() {
        this.tilstand = BehandlingTilstand.FERDIGSTILT
    }

    fun hentOpplysning(opplysningId: UUID): Opplysning {
        return opplysninger.singleOrNull { it.opplysningId == opplysningId }
            ?: throw IllegalArgumentException("Fant ikke opplysning med id $opplysningId")
    }

    fun tilstand(): BehandlingTilstand {
        return tilstand
    }

    fun journalpostId(): String? {
        return journalpostId
    }

    fun avbryt() {
        if (tilstand in setOf(BehandlingTilstand.FERDIGSTILT, BehandlingTilstand.OVERSEND_KLAGEINSTANS)) {
            throw IllegalStateException("Kan ikke avbryte klagebehandling i tilstand $tilstand")
        }
        tilstand = BehandlingTilstand.AVBRUTT
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

    enum class BehandlingTilstand {
        BEHANDLES,
        OVERSEND_KLAGEINSTANS,
        FERDIGSTILT,
        AVBRUTT,
    }
}
