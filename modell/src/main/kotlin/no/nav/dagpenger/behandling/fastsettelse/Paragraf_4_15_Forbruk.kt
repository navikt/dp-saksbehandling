package no.nav.dagpenger.behandling.fastsettelse

import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.mengde.Enhet.Companion.arbeidsdager
import no.nav.dagpenger.behandling.mengde.Tid
import no.nav.dagpenger.behandling.rapportering.Arbeidsdag
import no.nav.dagpenger.behandling.rapportering.Periode
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor
import no.nav.dagpenger.behandling.visitor.PersonVisitor

internal class Paragraf_4_15_Forbruk(private val person: Person) : Fastsettelse<Paragraf_4_15_Forbruk>(IkkeVurdert) {

    lateinit var forbruk: Tid

    object IkkeVurdert : Tilstand.IkkeVurdert<Paragraf_4_15_Forbruk>()

    override fun accept(visitor: FastsettelseVisitor) {
        tilstand.accept(this, visitor)
    }

    override fun håndter(rapporteringsHendelse: RapporteringsHendelse) {
        val periode = rapporteringsHendelse.tilPeriode()
        this.forbruk = ForbrukTeller(person, periode).forbruk
        endreTilstand(Vurdert)
    }

    object Vurdert : Tilstand.Vurdert<Paragraf_4_15_Forbruk>() {
        override fun accept(paragraf: Paragraf_4_15_Forbruk, visitor: FastsettelseVisitor) {
            visitor.visitForbruk(paragraf.forbruk)
        }
    }

    private class ForbrukTeller(person: Person, private val periode: Periode) : PersonVisitor {

        var forbruk = 0.arbeidsdager
        init {
            person.accept(this)
        }

        override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {
            if (arbeidsdag.erIPeriode(periode)) {
                forbruk += 1.arbeidsdager
            }
        }
    }
}
