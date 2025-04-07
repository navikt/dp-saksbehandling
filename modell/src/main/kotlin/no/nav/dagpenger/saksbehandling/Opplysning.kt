package no.nav.dagpenger.saksbehandling

import java.time.LocalDate
import java.util.UUID

class Opplysning(
    val id: UUID = UUIDv7.ny(),
    val template: OpplysningTemplate,
    var verdi: Verdi,
    val valgmuligheter: List<String> = emptyList(),
) {
    fun svar(verdi: Boolean) {
        when (val type = template.datatype) {
            Datatype.BOOLSK -> this.verdi = Verdi.Boolsk(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med boolsk verdi")
        }
    }

    fun svar(verdi: String) {
        when (val type = template.datatype) {
            Datatype.TEKST -> this.verdi = Verdi.TekstVerdi(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med tekst verdi")
        }
    }

    fun svar(verdi: List<String>) {
        when (val type = template.datatype) {
            Datatype.FLERVALG -> this.verdi = Verdi.Flervalg(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med liste verdi")
        }
    }

    fun svar(verdi: LocalDate) {
        when (val type = template.datatype) {
            Datatype.DATO -> this.verdi = Verdi.Dato(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med dato verdi")
        }
    }

    enum class Datatype {
        TEKST,
        DATO,
        BOOLSK,
        FLERVALG,
    }
}
