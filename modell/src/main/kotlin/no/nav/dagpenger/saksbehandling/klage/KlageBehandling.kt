package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.ER_KLAGEN_SKRIFTLIG
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.ER_KLAGEN_UNDERSKREVET
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_1
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_2
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_3
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_LAND
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_NAVN
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_POSTNR
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_POSTSTED
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HJEMLER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HVEM_KLAGER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.INTERN_MELDING
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST_OPPFYLT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER_VEDTAK
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_NEVNER_ENDRING
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGE_MOTTATT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.OPPREISNING_OVERSITTET_FRIST
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.RETTSLIG_KLAGEINTERESSE
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.VURDERNIG_AV_KLAGEN
import java.time.LocalDate
import java.util.UUID

object OpplysningBygger {
    val klagenGjelderOpplysningTyper =
        setOf(
            KLAGEN_GJELDER,
            KLAGEN_GJELDER_VEDTAK,
        )

    val fristvurderingOpplysningTyper =
        setOf(
            KLAGEFRIST,
            KLAGE_MOTTATT,
            KLAGEFRIST_OPPFYLT,
        )

    val oversittetFristOpplysningTyper =
        setOf(
            OPPREISNING_OVERSITTET_FRIST,
            OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE,
        )

    val formkravOpplysningTyper =
        setOf(
            ER_KLAGEN_SKRIFTLIG,
            ER_KLAGEN_UNDERSKREVET,
            KLAGEN_NEVNER_ENDRING,
            RETTSLIG_KLAGEINTERESSE,
        )

    val utfallOpplysningTyper =
        setOf(
            UTFALL,
            VURDERNIG_AV_KLAGEN,
        )

    val tilKlageinstansOpplysningTyper =
        setOf(
            HVEM_KLAGER,
            HJEMLER,
            INTERN_MELDING,
        )

    val fullmektigTilKlageinstansOpplysningTyper =
        setOf(
            FULLMEKTIG_NAVN,
            FULLMEKTIG_ADRESSE_1,
            FULLMEKTIG_ADRESSE_2,
            FULLMEKTIG_ADRESSE_3,
            FULLMEKTIG_POSTNR,
            FULLMEKTIG_POSTSTED,
            FULLMEKTIG_LAND,
        )

    fun lagOpplysninger(vararg opplysninger: OpplysningType): Set<Opplysning> = lagOpplysninger(opplysninger.toSet())

    fun lagOpplysninger(opplysninger: Set<OpplysningType> = emptySet()): Set<Opplysning> {
        return opplysninger.map {
            Opplysning(
                id = UUIDv7.ny(),
                type = it,
                verdi = Verdi.TomVerdi,
            )
        }.toSet()
    }
}

data class KlageBehandling(
    val behandlingId: UUID = UUIDv7.ny(),
    private val opplysninger: Set<Opplysning> = OpplysningBygger.lagOpplysninger(OpplysningType.entries.toSet()),
    private val steg: List<Steg> =
        listOf(
            KlagenGjelderSteg,
            FristvurderingSteg,
            FormkravSteg,
            VurderUtfallSteg,
            OversendKlageinstansSteg,
            FullmektigSteg,
        ),
    private var behandlingTilstand: BehandlingTilstand = BehandlingTilstand.KLAR_TIL_BEHANDLING,
) {
    init {
        steg.forEach { it.evaluerSynlighet(opplysninger) }
    }

    fun utfall(): UtfallType? {
        return opplysninger
            .single { it.type == UTFALL }
            .verdi
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

    fun svar(
        opplysningId: UUID,
        svar: Boolean,
    ) {
        hentOpplysning(opplysningId).also { opplysning ->
            opplysning.svar(svar)
            evaluerSynlighetOgUtfall()
        }
    }

    fun svar(
        opplysningId: UUID,
        svar: String,
    ) {
        hentOpplysning(opplysningId).also { opplysning ->
            opplysning.svar(svar)
            evaluerSynlighetOgUtfall()
        }
    }

    fun svar(
        opplysningId: UUID,
        svar: LocalDate,
    ) {
        hentOpplysning(opplysningId).also { opplysning ->
            opplysning.svar(svar)
            evaluerSynlighetOgUtfall()
        }
    }

    fun svar(
        opplysningId: UUID,
        svar: List<String>,
    ) {
        hentOpplysning(opplysningId).also { opplysning ->
            opplysning.svar(svar)
            evaluerSynlighetOgUtfall()
        }
    }

    fun kanFerdigstilles(): Boolean {
        val tommeOpplysninger =
            opplysninger.filter {
                it.synlighet() && it.type.påkrevd && it.verdi == Verdi.TomVerdi
            }
        return tommeOpplysninger.isEmpty()
    }

    fun ferdigstill() {
        if (!kanFerdigstilles()) {
            throw IllegalStateException("Kan ikke ferdigstille klagebehandling, opplysninger er ikke utfylt")
        }
        this.behandlingTilstand = BehandlingTilstand.FERDIGSTILT
    }

    private fun evaluerSynlighetOgUtfall() {
        this.steg.forEach { steg ->
            steg.evaluerSynlighet(opplysninger)
        }
    }

    fun hentOpplysning(opplysningId: UUID): Opplysning {
        return opplysninger.singleOrNull { it.id == opplysningId }
            ?: throw IllegalArgumentException("Fant ikke opplysning med id $opplysningId")
    }

    fun hentTilstand(): BehandlingTilstand {
        return behandlingTilstand
    }

    fun avbryt() {
        if (this.behandlingTilstand == BehandlingTilstand.FERDIGSTILT) {
            throw IllegalStateException("Kan ikke avbryte klagebehandling, den er allerede ferdigstilt")
        }
        this.behandlingTilstand = BehandlingTilstand.AVBRUTT
    }

    enum class BehandlingTilstand {
        KLAR_TIL_BEHANDLING,
        FERDIGSTILT,
        AVBRUTT,
    }
}
