package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.OpplysningerBygger.fristvurderingOpplysningTyper
import no.nav.dagpenger.saksbehandling.OpplysningerBygger.lagOpplysninger
import no.nav.dagpenger.saksbehandling.OpplysningerBygger.oversittetFristOpplysningTyper
import no.nav.dagpenger.saksbehandling.OpplysningerBygger.utfallOpplysningTyper
import java.time.LocalDate
import java.util.UUID

interface Steg {
    // fun opplysninger(): List<Opplysning>
    fun reevaluerOpplysngninger(opplysinger: List<Opplysning>)
}

class FristvurderingSteg : Steg {
    val fristvurderingOpplysninger = lagOpplysninger(fristvurderingOpplysningTyper)
    val oversittetFristOpplysninger = lagOpplysninger(oversittetFristOpplysningTyper)

    override fun reevaluerOpplysngninger(opplysinger: List<Opplysning>) {
        when (klagefristOppfylt(opplysinger)) {
            true -> opplysinger.filter { it.type in oversittetFristOpplysningTyper }.forEach { it.settSynlighet(false) }
            false -> {
                opplysinger.filter { it.type in oversittetFristOpplysningTyper }.forEach { it.settSynlighet(true) }
            }
        }
    }

    private fun klagefristOppfylt(opplysinger: List<Opplysning>): Boolean {
        val klagefristOpplysning =
            opplysinger.single { opplysning -> opplysning.type == OpplysningType.KLAGEFRIST_OPPFYLT }
        return klagefristOpplysning.verdi is Verdi.Boolsk && (klagefristOpplysning.verdi as Verdi.Boolsk).value == true
    }
}

class VurderUtfallSteg : Steg {
    val utfallOpplysning = lagOpplysninger(utfallOpplysningTyper)

    override fun reevaluerOpplysngninger(opplysinger: List<Opplysning>) {
        TODO("Not yet implemented")
    }
}

class Opplysning(
    val id: UUID = UUIDv7.ny(),
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
