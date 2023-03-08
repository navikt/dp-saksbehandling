package no.nav.dagpenger.behandling.fastsettelse

import no.nav.dagpenger.behandling.hendelser.Rapporteringshendelse
import no.nav.dagpenger.behandling.mengde.Enhet.Companion.arbeidsdager
import no.nav.dagpenger.behandling.mengde.Tid
import no.nav.dagpenger.behandling.rapportering.Arbeidsdag
import no.nav.dagpenger.behandling.rapportering.Dag
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor

internal class Paragraf_4_15_Forbruk : Fastsettelse<Paragraf_4_15_Forbruk>(IkkeVurdert) {

    lateinit var forbruk: Tid

    object IkkeVurdert : Tilstand.IkkeVurdert<Paragraf_4_15_Forbruk>() {
        override fun håndter(
            rapporteringsHendelse: Rapporteringshendelse,
            tellendeDager: List<Dag>,
            fastsettelse: Paragraf_4_15_Forbruk,
        ) {
            fastsettelse.forbruk = tellendeDager.filterIsInstance<Arbeidsdag>().size.arbeidsdager
            fastsettelse.endreTilstand(Vurdert)
        }
    }
    object Vurdert : Tilstand.Vurdert<Paragraf_4_15_Forbruk>() {
        override fun accept(paragraf: Paragraf_4_15_Forbruk, visitor: FastsettelseVisitor) {
            visitor.visitForbruk(paragraf.forbruk)
        }
    }

    override fun accept(visitor: FastsettelseVisitor) {
        tilstand.accept(this, visitor)
    }

    override fun <T> implementasjon(block: Paragraf_4_15_Forbruk.() -> T) = this.block()
}
