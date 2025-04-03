package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.Utfall.TomtUtfall
import java.time.LocalDate
import java.util.UUID

class KlageBehandling(
    val id: UUID,
    val person: Person,
) {
    private var _utfall: Utfall = TomtUtfall

    private val klagesakGruppe =
        Gruppe(
            navn = GrupperNavn.KLAGESAK,
            opplysninger =
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
    private val fristGruppe =
        Gruppe(
            navn = GrupperNavn.FRIST,
            opplysninger =
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

    private val formkravGruppe =
        Gruppe(
            navn = GrupperNavn.FORMKRAV,
            opplysninger =
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
    private val oversendelseGruppe =
        Gruppe(
            navn = GrupperNavn.OVERSENDELSE,
            opplysninger =
                setOf(
                    Opplysning(
                        navn = "Hvilke hjemler gjelder klagen",
                        type = Opplysning.OpplysningType.FLERVALG,
                        verdi = Verdi.TomVerdi,
                    ),
                ),
        )

    val grupper = setOf(klagesakGruppe, fristGruppe, formkravGruppe, oversendelseGruppe)
    val opplysninger: Set<Opplysning> =
        grupper
            .flatMap { it.opplysninger }
            .toSet()

    val utfall: Utfall get() = _utfall

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
        formkravGruppe.opplysninger.filter { it.verdi is Verdi.Boolsk }.any {
            !(it.verdi as Verdi.Boolsk).value
        }.let {
            when (it) {
                true -> this._utfall = Utfall.Avvist
                false -> {}
            }
        }
    }
}

enum class GrupperNavn {
    FORMKRAV,
    KLAGESAK,
    FRIST,
    OVERSENDELSE,
}

class Gruppe(
    val navn: GrupperNavn,
    val opplysninger: Set<Opplysning>,
)

sealed class Verdi {
    data object TomVerdi : Verdi()

    data class TekstVerdi(val value: String) : Verdi()

    data class Dato(val value: LocalDate) : Verdi()

    data class Boolsk(val value: Boolean) : Verdi()

    data class Flervalg(val value: List<String>) : Verdi()
}

sealed class Utfall {
    data object TomtUtfall : Utfall()

    data object Avvist : Utfall()
}
