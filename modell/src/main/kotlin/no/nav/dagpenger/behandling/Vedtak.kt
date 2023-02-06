package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.visitor.PersonVisitor
import java.time.LocalDate
import java.time.LocalDateTime

class Vedtak private constructor(private val utfall: Boolean, private val virkningsdato: LocalDate, private val vedtakstidspunkt: LocalDateTime) {
    fun accept(visitor: PersonVisitor) {
        visitor.visitVedtak(utfall)
    }

    constructor(utfall: Boolean, virkningsdato: LocalDate) : this(utfall, virkningsdato, LocalDateTime.now())
}
