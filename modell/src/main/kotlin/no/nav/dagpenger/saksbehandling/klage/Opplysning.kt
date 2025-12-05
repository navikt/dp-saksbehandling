package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.UUIDv7
import java.time.LocalDate
import java.util.UUID

class Opplysning(
    val opplysningId: UUID = UUIDv7.ny(),
    val type: OpplysningType,
    private var verdi: Verdi,
    private var synlig: Boolean = true,
    val valgmuligheter: List<String> = emptyList(),
    private val regler: Set<Regel> = emptySet(),
) {
    init {
        requireRiktigDatatype(verdi)
        requireReglerOppfylt()
    }

    fun verdi(): Verdi = this.verdi

    fun settSynlighet(synlig: Boolean) {
        when (synlig) {
            true -> this.synlig = true
            false -> {
                this.synlig = false
                this.verdi = Verdi.TomVerdi
            }
        }
    }

    fun synlighet(): Boolean = this.synlig

    fun svar(verdi: Verdi) {
        requireRiktigDatatype(verdi).also { this.verdi = it }
        requireReglerOppfylt()
    }

    private fun requireReglerOppfylt() {
        val ikkeOppfylteRegler =
            this.regler
                .map { it.erOppfylt(this) }
                .filterIsInstance<Regel.Resultat.IkkeOppfylt>()
                .map { it.begrunnelse }

        require(ikkeOppfylteRegler.isEmpty()) {
            "Regler ikke oppfylt: ${ikkeOppfylteRegler.joinToString()}"
        }
    }

    private fun requireRiktigDatatype(verdi: Verdi): Verdi {
        when (verdi) {
            is Verdi.Boolsk -> {
                require(
                    type.datatype == Datatype.BOOLSK,
                ) { "Opplysning av type ${type.datatype} kan ikke ha verdi av type ${verdi::class.simpleName}" }
            }

            is Verdi.Dato -> {
                require(
                    type.datatype == Datatype.DATO,
                ) { "Opplysning av type ${type.datatype} kan ikke ha verdi av type ${verdi::class.simpleName}" }
            }

            is Verdi.Flervalg -> {
                require(
                    type.datatype == Datatype.FLERVALG,
                ) { "Opplysning av type ${type.datatype} kan ikke ha verdi av type ${verdi::class.simpleName}" }
                if (valgmuligheter.isNotEmpty()) {
                    require(
                        verdi.value.all {
                            it in valgmuligheter
                        },
                    ) { "Opplysningene må være av lovlige valgmuligheter $valgmuligheter. Verdi er ${verdi.value}" }
                }
            }

            is Verdi.TekstVerdi -> {
                require(
                    type.datatype == Datatype.TEKST,
                ) { "Opplysning av type ${type.datatype} kan ikke ha verdi av type ${verdi::class.simpleName}" }

                if (valgmuligheter.isNotEmpty()) {
                    require(
                        verdi.value in valgmuligheter,
                    ) { "Opplysningene må være av lovlige valgmuligheter $valgmuligheter. Verdi er ${verdi.value}" }
                }
            }

            Verdi.TomVerdi -> {}
        }
        return verdi
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Opplysning) return false

        return opplysningId == other.opplysningId && type == other.type
    }

    override fun hashCode(): Int = 31 * opplysningId.hashCode() + type.hashCode()
}

private inline fun Opplysning.require(
    value: Boolean,
    lazyMessage: () -> String,
) {
    if (!value) {
        throw UgyldigOpplysningException(
            opplysning = this,
            melding = lazyMessage(),
        )
    }
}

class UgyldigOpplysningException(
    opplysning: Opplysning,
    melding: String = "Ugyldig opplysning",
) : IllegalArgumentException("$melding: ${opplysning.type.navn} med verdi ${opplysning.verdi()}")

sealed class Verdi {
    data object TomVerdi : Verdi()

    data class TekstVerdi(
        val value: String,
    ) : Verdi()

    data class Dato(
        val value: LocalDate,
    ) : Verdi()

    data class Boolsk(
        val value: Boolean,
    ) : Verdi()

    data class Flervalg(
        val value: List<String>,
    ) : Verdi() {
        constructor(vararg value: String) : this(value.toList())
    }
}
