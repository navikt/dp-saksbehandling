package no.nav.dagpenger.saksbehandling.pdl

import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import java.time.LocalDate

data class PDLPersonIntern(
    val ident: String,
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?,
    val fødselsdato: LocalDate,
    val alder: Int,
    val statsborgerskap: String?,
    val kjønn: PDLPerson.Kjonn,
    val adresseBeskyttelseGradering: AdressebeskyttelseGradering,
)
