package no.nav.dagpenger.saksbehandling

import java.time.LocalDate
import java.util.UUID

interface KlageMediator {
    // todo Domenemodell
    fun hentKlage(klageId: java.util.UUID): no.nav.dagpenger.saksbehandling.api.models.KlageDTO

    fun oppdaterKlageOpplysning(
        klageId: UUID,
        opplysningId: UUID,
        verdi: OpplysningerVerdi,
    )
}

object NotImplementKlageMediator : KlageMediator {
    override fun hentKlage(klageId: UUID): no.nav.dagpenger.saksbehandling.api.models.KlageDTO {
        throw NotImplementedError("Not implemented")
    }

    override fun oppdaterKlageOpplysning(
        klageId: UUID,
        opplysningId: UUID,
        verdi: OpplysningerVerdi,
    ) {
        throw NotImplementedError("Not implemented")
    }
}

data class Opplysning(
    val id: UUID,
    val verdi: OpplysningerVerdi,
)

sealed class OpplysningerVerdi {
    data class Tekst(val value: String) : OpplysningerVerdi()

    data class TekstListe(private val value: List<String> = emptyList()) : OpplysningerVerdi(), List<String> by value {
        constructor(vararg values: String) : this(values.toList())
    }

    data class Dato(val value: LocalDate) : OpplysningerVerdi()

    data class Boolsk(val value: Boolean) : OpplysningerVerdi()
}
