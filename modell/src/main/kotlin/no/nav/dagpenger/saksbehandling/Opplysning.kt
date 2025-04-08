package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.OpplysningerBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.OpplysningerBygger.lagOpplysninger
import java.time.LocalDate
import java.util.UUID

interface Steg {
    fun hentOpplysninger(): List<Opplysning>
}

class FristvurderingSteg : Steg {
    val opplysninger = lagOpplysninger(fristvurderingOpplysningTyper)

    override fun hentOpplysninger(): List<Opplysning> {
        return opplysninger.toList()
    }
}

class Opplysning(
    val id: UUID = UUIDv7.ny(),
    val type: OpplysningType,
    var verdi: Verdi,
    val valgmuligheter: List<String> = emptyList(),
) {
    fun svar(verdi: Boolean) {
        when (val type = type.datatype) {
            Datatype.BOOLSK -> this.verdi = Verdi.Boolsk(verdi)
            else -> throw IllegalArgumentException("Opplysning av type $type kan ikke besvares med boolsk verdi")
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

    enum class Datatype {
        TEKST,
        DATO,
        BOOLSK,
        FLERVALG,
    }
}
