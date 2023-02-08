package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.visitor.PersonVisitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Vedtak private constructor(
    private val vedtakId: UUID,
    private val utfall: Boolean,
    private val virkningsdato: LocalDate,
    private val vedtakstidspunkt: LocalDateTime,
) {

    private constructor(utfall: Boolean, virkningsdato: LocalDate) : this(
        UUID.randomUUID(),
        utfall,
        virkningsdato,
        LocalDateTime.now()
    )

    companion object {
        fun avslag(virkningsdato: LocalDate) = Vedtak(utfall = false, virkningsdato = virkningsdato)
        fun innvilgelse(virkningsdato: LocalDate) = Vedtak(utfall = true, virkningsdato = virkningsdato)
    }
    fun accept(visitor: PersonVisitor) {
        visitor.visitVedtak(utfall)
    }
}
