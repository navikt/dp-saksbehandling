package no.nav.dagpenger.behandling.rapportering

import no.nav.dagpenger.behandling.Dagpengerettighet
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.entitet.Periode
import no.nav.dagpenger.behandling.visitor.PersonVisitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class TellendeDager(person: Person, val periode: Periode) : PersonVisitor {

    private val dager = mutableListOf<Dag>()
    lateinit var virkningsdato: LocalDate
    private var gyldigTom: LocalDate? = null
    var harDagpengevedtak = false

    init {
        person.accept(this)
    }

    fun tellendeDager() = dager.filter { dato -> dato >= virkningsdato && (gyldigTom == null || dato <= gyldigTom!!) }

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {
        if (arbeidsdag in periode) {
            dager.add(arbeidsdag)
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
        gyldigTom: LocalDate?,
    ) {
        if (harDagpengevedtak) {
            this.virkningsdato = virkningsdato
            if (gyldigTom != null) {
                this.gyldigTom = gyldigTom
            }
        }
        harDagpengevedtak = false
    }

    override fun visitHelgedag(helgedag: Helgedag) {
        if (helgedag in periode) {
            dager.add(helgedag)
        }
    }
}
