package no.nav.dagpenger.saksbehandling

import java.time.LocalDate
import java.util.UUID

class Opplysning(
    val id: UUID = UUIDv7.ny(),
    val navn: String,
    val type: OpplysningType,
    var verdi: Verdi,
) {
    fun svar(verdi: Boolean) {
        when (type) {
            OpplysningType.BOOLSK -> this.verdi = Verdi.Boolsk(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med boolsk verdi")
        }
    }

    fun svar(verdi: String) {
        when (type) {
            OpplysningType.TEKST -> this.verdi = Verdi.TekstVerdi(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med tekst verdi")
        }
    }

    fun svar(verdi: List<String>) {
        when (type) {
            OpplysningType.FLERVALG -> this.verdi = Verdi.Flervalg(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med liste verdi")
        }
    }

    fun svar(verdi: LocalDate) {
        when (type) {
            OpplysningType.DATO -> this.verdi = Verdi.Dato(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med dato verdi")
        }
    }

    enum class OpplysningType {
        TEKST,
        DATO,
        BOOLSK,
        FLERVALG,
    }
}
