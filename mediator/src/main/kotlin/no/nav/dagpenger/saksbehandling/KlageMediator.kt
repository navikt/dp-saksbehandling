package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.api.models.KlageDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTO
import java.time.LocalDate
import java.util.UUID

class KlageMediator {
    // todo Domenemodell
    fun hentKlage(klageId: java.util.UUID): no.nav.dagpenger.saksbehandling.api.models.KlageDTO {
        val personId = "12345678901" // Todo: hent personen klagen gjelder
        val klage =
            KlageBehandling(
                klageId,
                person =
                    Person(
                        ident = personId,
                        skjermesSomEgneAnsatte = false,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                    ),
            )
        return KlageDTO(
            id = klageId,
            behandlingOpplysninger =
                klage.grupper.flatMap { gruppe ->
                    gruppe.opplysninger.map {
                        KlageOpplysningDTO(
                            id = it.id,
                            navn = "Test",
                            type = KlageOpplysningDTO.Type.TEKST,
                            verdi = null,
                            paakrevd = true,
                            gruppe = KlageOpplysningDTO.Gruppe.valueOf(gruppe.navn.name),
                            redigerbar = true,
                            valgmuligheter = emptyList(),
                        )
                    }
                },
            utfallOpplysninger = listOf(),
            utfall =
                UtfallDTO(
                    verdi = UtfallDTO.Verdi.IKKE_SATT,
                    tilgjeneligeUtfall =
                        listOf(
                            UtfallDTO.Verdi.AVVIST.toString(),
                            UtfallDTO.Verdi.MEDHOLD.toString(),
                            UtfallDTO.Verdi.DELVISMinusMEDHOLD.toString(),
                        ),
                ),
            saksbehandler = null,
            meldingOmVedtak = null,
        )
    }

    fun oppdaterKlageOpplysning(
        klageId: UUID,
        opplysningId: UUID,
        verdi: OpplysningerVerdi,
    ) {
        return when (verdi) {
            is OpplysningerVerdi.Tekst -> TODO("Not yet implemented")
            is OpplysningerVerdi.TekstListe -> TODO("Not yet implemented")
            is OpplysningerVerdi.Dato -> TODO("Not yet implemented")
            is OpplysningerVerdi.Boolsk -> TODO("Not yet implemented")
        }
    }
}
/*
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
}*/

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
