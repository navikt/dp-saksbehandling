package no.nav.dagpenger.behandling.rapportering

import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.entitet.Dagpengerettighet
import no.nav.dagpenger.behandling.entitet.Periode
import no.nav.dagpenger.behandling.visitor.PersonVisitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class TellendeDager(person: Person, val periode: Periode) : PersonVisitor {

    private val arbeidsdager = mutableListOf<Dag>()
    lateinit var virkningsdato: LocalDate
    var harDagpengevedtak = false

    init {
        person.accept(this)
    }

    fun tellendeDager() = arbeidsdager.filter { it >= virkningsdato }

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {
        if (arbeidsdag in periode) {
            arbeidsdager.add(arbeidsdag)
        }
    }

    override fun visitVedtakDagpengerettighet(dagpengerettighet: Dagpengerettighet) {
        harDagpengevedtak = true
    }

    override fun postVisitVedtak(
        vedtakId: UUID,
        virkningsdato: LocalDate,
        vedtakstidspunkt: LocalDateTime,
        utfall: Boolean,
    ) {
        if (harDagpengevedtak) {
            this.virkningsdato = virkningsdato
        }
        harDagpengevedtak = false
    }

    override fun visitHelgedag(helgedag: Helgedag) {
        if (helgedag in periode) {
            arbeidsdager.add(helgedag)
        }
    }
}
