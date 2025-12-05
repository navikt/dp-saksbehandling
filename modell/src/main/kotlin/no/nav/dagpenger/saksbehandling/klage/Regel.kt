package no.nav.dagpenger.saksbehandling.klage

import java.time.LocalDate

interface Regel {
    fun erOppfylt(opplysning: Opplysning): Resultat

    sealed class Resultat {
        data object Oppfylt : Resultat()

        data class IkkeOppfylt(
            val begrunnelse: String,
        ) : Resultat()
    }
}

object IkkeIFremtiden : Regel {
    override fun erOppfylt(opplysning: Opplysning): Regel.Resultat {
        val terskelDato = LocalDate.now()
        return when (val verdi = opplysning.verdi()) {
            is Verdi.Dato -> {
                if (verdi.value.isBefore(LocalDate.now()) || verdi.value.isEqual(terskelDato)) {
                    Regel.Resultat.Oppfylt
                } else {
                    Regel.Resultat.IkkeOppfylt(
                        "Dato ${verdi.value} er ikke før terskeldato $terskelDato",
                    )
                }
            }
            is Verdi.TomVerdi -> Regel.Resultat.Oppfylt
            else -> Regel.Resultat.IkkeOppfylt("Opplysningen må være av type Dato")
        }
    }
}
