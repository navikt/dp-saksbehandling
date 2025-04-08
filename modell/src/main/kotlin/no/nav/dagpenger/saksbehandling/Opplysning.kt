package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.OpplysningerBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.OpplysningerBygger.lagOpplysninger
import no.nav.dagpenger.saksbehandling.OpplysningerBygger.oversittetFristOpplysningTyper
import java.time.LocalDate
import java.util.UUID

interface Steg {
    fun opplysninger(): List<Opplysning>
}

class FristvurderingSteg : Steg {
    val fristvurderingOpplysninger = lagOpplysninger(fristvurderingOpplysningTyper)
    val oversittetFristOpplysninger = lagOpplysninger(oversittetFristOpplysningTyper)

    override fun opplysninger(): List<Opplysning> {
        return when (klagefristOppfylt()) {
            true -> fristvurderingOpplysninger.toList()
            false -> fristvurderingOpplysninger.toList() + oversittetFristOpplysninger.toList()
        }
    }

    private fun klagefristOppfylt(): Boolean {
        val klagefristOpplysning = fristvurderingOpplysninger.single { opplysning -> opplysning.type == OpplysningType.KLAGEFRIST_OPPFYLT }
        return klagefristOpplysning.verdi is Verdi.Boolsk && (klagefristOpplysning.verdi as Verdi.Boolsk).value == true
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
