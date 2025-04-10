package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.*
import no.nav.dagpenger.saksbehandling.klage.Utfall.TomtUtfall
import java.time.LocalDate
import java.util.UUID

object OpplysningerBygger {
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

    val klagenGjelserOpplysningTyper =
        setOf(
            KLAGEN_GJELDER,
        )

    val oversendelseKlageinnstansOpplysningTyper =
        setOf(
            HVEM_KLAGER,
            HJEMLER,
            INTERN_MELDING
        )
    val fullmektigTilKlageInnstansOpplysningTyper =
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
            FristvurderingSteg(),
            FormkravSteg,
            VurderUtfallSteg(),
        ),
) {
    private var _utfall: Utfall = TomtUtfall

    val utfall: Utfall get() = _utfall

    fun synligeOpplysninger(): Set<Opplysning> {
        return opplysninger.filter { it.synlighet() }.toSet()
    }

    fun settUtfall(utfall: Utfall) {
        this._utfall = utfall
    }

    fun hentUtfallOpplysninger(): Set<Opplysning> {
        return if (utfall == TomtUtfall) {
            emptySet()
        } else {
            emptySet()
        }
    }

    fun svar(
        opplysninngId: UUID,
        svar: Boolean,
    ) {
        hentOpplysning(opplysninngId).also { opplysning ->
            opplysning.svar(svar)
            this.revurderUtfall()
        }
    }

    fun svar(
        opplysninngId: UUID,
        svar: String,
    ) {
        hentOpplysning(opplysninngId).also { opplysning ->
            opplysning.svar(svar)
            this.revurderUtfall()
        }
    }

    fun svar(
        opplysninngId: UUID,
        svar: LocalDate,
    ) {
        hentOpplysning(opplysninngId).also { opplysning ->
            opplysning.svar(svar)
            this.revurderUtfall()
        }
    }

    fun svar(
        opplysninngId: UUID,
        svar: List<String>,
    ) {
        hentOpplysning(opplysninngId).also { opplysning ->
            opplysning.svar(svar)
            this.revurderUtfall()
        }
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
