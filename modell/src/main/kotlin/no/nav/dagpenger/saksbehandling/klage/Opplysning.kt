package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.UUIDv7
import java.time.LocalDate
import java.util.UUID

class Opplysning(
    val opplysningId: UUID = UUIDv7.ny(),
    val type: OpplysningType,
    var verdi: Verdi,
    private var synlig: Boolean = true,
    val valgmuligheter: List<String> = emptyList(),
) {
    fun settSynlighet(synlig: Boolean) {
        when (synlig) {
            true -> this.synlig = true
            false -> {
                this.synlig = false
                this.verdi = Verdi.TomVerdi
            }
        }
    }

    fun synlighet(): Boolean {
        return this.synlig
    }

    fun svar(verdi: Boolean) {
        when (val datatype = type.datatype) {
            Datatype.BOOLSK -> this.verdi = Verdi.Boolsk(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $datatype kan ikke besvares med boolsk verdi")
        }
    }

    fun svar(verdi: String) {
        when (val type = type.datatype) {
            Datatype.TEKST -> this.verdi = Verdi.TekstVerdi(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med tekst verdi")
        }
    }

    fun svar(verdi: List<String>) {
        when (val type = type.datatype) {
            Datatype.FLERVALG -> this.verdi = Verdi.Flervalg(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med liste verdi")
        }
    }

    fun svar(verdi: LocalDate) {
        when (val type = type.datatype) {
            Datatype.DATO -> this.verdi = Verdi.Dato(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med dato verdi")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Opplysning) return false

        return opplysningId == other.opplysningId && type == other.type
    }

    override fun hashCode(): Int {
        return 31 * opplysningId.hashCode() + type.hashCode()
    }
}

sealed class Verdi {
    data object TomVerdi : Verdi()

    data class TekstVerdi(val value: String) : Verdi()

    data class Dato(val value: LocalDate) : Verdi()

    data class Boolsk(val value: Boolean) : Verdi()

    data class Flervalg(val value: List<String>) : Verdi() {
        constructor(vararg value: String) : this(value.toList())
    }
}
