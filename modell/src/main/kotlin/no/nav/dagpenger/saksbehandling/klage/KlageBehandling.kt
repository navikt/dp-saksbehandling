package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.Person
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
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGE_MOTTATT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.OPPREISNING_OVERSITTET_FRIST
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.VURDERNIG_AV_KLAGEN
import no.nav.dagpenger.saksbehandling.klage.Utfall.TomtUtfall
import java.time.LocalDate
import java.util.UUID

object OpplysningerBygger {
    val klagenGjelderOpplysningTyper =
        setOf(
            KLAGEN_GJELDER,
            KLAGEN_GJELDER_VEDTAK,
        )

    val formkravOpplysningTyper =
        setOf(
            ER_KLAGEN_SKRIFTLIG,
            ER_KLAGEN_UNDERSKREVET,
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

    fun lagOpplysninger(opplysninger: Set<OpplysningType> = emptySet()): Set<Opplysning> {
        return opplysninger.map {
            Opplysning(
                id = UUID.randomUUID(),
                type = it,
                verdi = Verdi.TomVerdi,
            )
        }.toSet()
    }
}

class KlageBehandling(
    val id: UUID = UUIDv7.ny(),
    val person: Person,
    private val opplysninger: Set<Opplysning> = OpplysningerBygger.lagOpplysninger(OpplysningType.entries.toSet()),
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

    private var _utfall: Utfall = TomtUtfall

    val utfall: Utfall get() = _utfall

    fun synligeOpplysninger(): Set<Opplysning> {
        return opplysninger.filter { it.synlighet() }.toSet()
    }

    fun settUtfall(utfall: Utfall) {
        this._utfall = utfall
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

    private fun evaluerSynlighetOgUtfall() {
        this.steg.forEach { steg ->
            steg.evaluerSynlighet(opplysninger)
        }
        this.revurderUtfall()
    }

    fun hentOpplysning(opplysningId: UUID): Opplysning {
        return opplysninger.singleOrNull { it.id == opplysningId }
            ?: throw IllegalArgumentException("Fant ikke opplysning med id $opplysningId")
    }

    private fun revurderUtfall() {
        opplysninger.filter { it.type in setOf(ER_KLAGEN_UNDERSKREVET, ER_KLAGEN_SKRIFTLIG) }
            .filter { it.verdi is Verdi.Boolsk }
            .any { !(it.verdi as Verdi.Boolsk).value }
            .let {
                when (it) {
                    true -> this._utfall = Utfall.Avvist
                    false -> {}
                }
            }
    }

    fun hentSteg(): List<Steg> = this.steg.toList()
}

sealed class Utfall {
    data object TomtUtfall : Utfall()

    data object Avvist : Utfall()

    data object Opprettholdelse : Utfall()
}
