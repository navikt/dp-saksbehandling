package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.Utfall.TomtUtfall
import java.time.LocalDate
import java.util.UUID

class KlageBehandling(
    val id: UUID,
    val person: Person,
) {
    var utfall: Utfall = TomtUtfall

    fun svar(
        opplysninngId: UUID,
        b: Boolean,
    ) {
        grupper.flatMap { it.opplysninger }.singleOrNull { it.id == opplysninngId }?.also { opplysning ->
            opplysning.svar(b)
            this.revurderUtfall()
        }
    }

    private fun revurderUtfall() {
        formkravGruppe.opplysninger.filter { it.verdi is Verdi.Boolse }.any {
            !(it.verdi as Verdi.Boolse).value
        }.let {
            when (it) {
                true -> this.utfall = Utfall.Avvist
                false -> {}
            }
        }
    }

    val klagesakGruppe =
        Gruppe(
            GrupperNavn.KLAGESAK,
            setOf(
                Opplysning(
                    navn = "Hva klagen gjelder",
                    type = Opplysning.OpplysningType.TEKST,
                    verdi = Verdi.TomVerdi,
                ),
                Opplysning(
                    navn = "Vedtak klagen gjelder",
                    type = Opplysning.OpplysningType.TEKST,
                    verdi = Verdi.TomVerdi,
                ),
            ),
        )
    val fristGruppe =
        Gruppe(
            GrupperNavn.FRIST,
            setOf(
                Opplysning(
                    navn = "Frist for å klage",
                    type = Opplysning.OpplysningType.DATO,
                    verdi = Verdi.TomVerdi,
                ),
                Opplysning(
                    navn = "Frist mottatt",
                    type = Opplysning.OpplysningType.DATO,
                    verdi = Verdi.TomVerdi,
                ),
                Opplysning(
                    navn = "Har klager klaget innen fristen",
                    type = Opplysning.OpplysningType.BOOLSK,
                    verdi = Verdi.TomVerdi,
                ),
            ),
        )

    val formkravGruppe =
        Gruppe(
            GrupperNavn.FORMKRAV,
            setOf(
                Opplysning(
                    navn = "Er klagen skriftlig",
                    type = Opplysning.OpplysningType.BOOLSK,
                    verdi = Verdi.TomVerdi,
                ),
                Opplysning(
                    navn = "Er klagen underskrevet",
                    type = Opplysning.OpplysningType.BOOLSK,
                    verdi = Verdi.TomVerdi,
                ),
                Opplysning(
                    navn = "Nevner klagen den endring som krevest",
                    type = Opplysning.OpplysningType.BOOLSK,
                    verdi = Verdi.TomVerdi,
                ),
                Opplysning(
                    navn = "Har klager rettslig klageinteresse",
                    type = Opplysning.OpplysningType.BOOLSK,
                    verdi = Verdi.TomVerdi,
                ),
            ),
        )

    val grupper: Set<Gruppe> =
        setOf(
            klagesakGruppe,
            formkravGruppe,
            fristGruppe,
        )
}

class Opplysning(
    val id: UUID = UUIDv7.ny(),
    val navn: String,
    val type: OpplysningType,
    var verdi: Verdi,
) {
    fun svar(b: Boolean) {
        when (type) {
            OpplysningType.BOOLSK -> verdi = Verdi.Boolse(b)
            else -> throw IllegalArgumentException("Kan ikke svare på en opplysning av type $type")
        }
    }

    enum class OpplysningType {
        TEKST,
        DATO,
        BOOLSK,
    }
}

enum class GrupperNavn {
    FORMKRAV,
    KLAGESAK,
    FRIST,
}

class Gruppe(
    val navn: GrupperNavn,
    val opplysninger: Set<Opplysning>,
)

sealed class Verdi() {
    object TomVerdi : Verdi()

    data class TekstVerdi(val value: String) : Verdi()

    data class Dato(val value: LocalDate) : Verdi()

    data class Boolse(val value: Boolean) : Verdi()
}

sealed class Utfall {
    object TomtUtfall : Utfall()

    object Avvist : Utfall()
}
