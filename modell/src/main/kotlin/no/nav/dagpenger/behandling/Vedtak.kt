package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.visitor.PersonVisitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Vedtak private constructor(private val vedtakId: UUID, private val utfall: Boolean, private val virkningsdato: LocalDate, private val vedtakstidspunkt: LocalDateTime) {
    fun accept(visitor: PersonVisitor) {
        visitor.visitVedtak(utfall)
    }

    constructor(utfall: Boolean, virkningsdato: LocalDate) : this(UUID.randomUUID(), utfall, virkningsdato, LocalDateTime.now())
}
