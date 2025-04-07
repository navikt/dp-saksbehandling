package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.Opplysning.Datatype
import no.nav.dagpenger.saksbehandling.Utfall.TomtUtfall
import java.time.LocalDate
import java.util.UUID

enum class OpplysningTemplate(
    val navn: String,
    val datatype: Datatype,
) {
    ER_KLAGEN_SKRIFTLIG(
        navn = "Er klagen skriftlig",
        datatype = Datatype.BOOLSK,
    ),

    ER_KLAGEN_UNDERSKREVET(
        navn = "Er klagen underskrevet",
        datatype = Datatype.BOOLSK,
    ),

    KLAGE_FRIST(
        navn = "Frist for å klage",
        datatype = Datatype.DATO,
    ),

    KLAGEN_GJELDER(
        navn = "Hva klagen gjelder",
        datatype = Datatype.FLERVALG,
    ),

    FRIST_SAKSBEHANDLERS_BEGRUNNELSE(
        navn = "Saksbehandlerens begrunnelse for frist",
        datatype = Datatype.TEKST,
    ),
}

object OpplysningerBygger {
    val opplysninger =
        setOf(
            OpplysningTemplate.ER_KLAGEN_SKRIFTLIG,
            OpplysningTemplate.ER_KLAGEN_UNDERSKREVET,
            OpplysningTemplate.KLAGE_FRIST,
            OpplysningTemplate.KLAGEN_GJELDER,
            OpplysningTemplate.FRIST_SAKSBEHANDLERS_BEGRUNNELSE,
        )

    fun lagOpplysninger(): Set<Opplysning> {
        return opplysninger.map {
            Opplysning(
                id = UUID.randomUUID(),
                template = it,
                verdi = Verdi.TomVerdi,
            )
        }.toSet()
    }
}

class KlageBehandling(
    val id: UUID,
    val person: Person,
    private val opplysninger: Set<Opplysning> = OpplysningerBygger.lagOpplysninger(),
) {
    private var _utfall: Utfall = TomtUtfall

    val utfall: Utfall get() = _utfall

    fun hentOpplysninger(): Set<Opplysning> {
        return opplysninger
    }

    fun settUtfall(utfall: Utfall) {
        this._utfall = utfall
    }

    fun hentUtfallOpplysninger(): Set<Opplysning> {
        if (utfall in setOf(Utfall.Opprettholdelse)){

        }
        return if (utfall == TomtUtfall) {
            emptySet()
        } else {
            emptySet()
        }
    }

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
        opplysninger.filter { it.template in setOf(OpplysningTemplate.ER_KLAGEN_UNDERSKREVET, OpplysningTemplate.ER_KLAGEN_SKRIFTLIG) }
            .filter { it.verdi is Verdi.Boolsk }
            .any { !(it.verdi as Verdi.Boolsk).value }
            .let {
                when (it) {
                    true -> this._utfall = Utfall.Avvist
                    false -> {}
                }
            }
    }
}

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

    data object Opprettholdelse : Utfall()

}
