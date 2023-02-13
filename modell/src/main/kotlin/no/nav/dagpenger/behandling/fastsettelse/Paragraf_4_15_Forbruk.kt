package no.nav.dagpenger.behandling.fastsettelse

import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor

internal class Paragraf_4_15_Forbruk(private val person: Person) : Fastsettelse<Paragraf_4_15_Forbruk>(IkkeVurdert) {
    object IkkeVurdert : Tilstand.IkkeVurdert<Paragraf_4_15_Forbruk>()

    override fun accept(visitor: FastsettelseVisitor) {
        TODO("Not yet implemented")
    }

    override fun håndter(rapporteringsHendelse: RapporteringsHendelse) {
         val periode = rapporteringsHendelse.tilPeriode()
    }
}
