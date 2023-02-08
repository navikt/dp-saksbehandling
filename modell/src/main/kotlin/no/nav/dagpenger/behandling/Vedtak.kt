package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.visitor.PersonVisitor
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Vedtak private constructor(
    private val vedtakId: UUID,
    private val utfall: Boolean,
    private val virkningsdato: LocalDate,
    private val vedtakstidspunkt: LocalDateTime,
    private val grunnlag: BigDecimal?,
    private val dagsats: BigDecimal?,
) {

    private constructor(
        utfall: Boolean,
        virkningsdato: LocalDate,
        grunnlag: BigDecimal? = null,
        dagsats: BigDecimal? = null,
    ) : this(
        vedtakId = UUID.randomUUID(),
        utfall = utfall,
        virkningsdato = virkningsdato,
        vedtakstidspunkt = LocalDateTime.now(),
        grunnlag = null,
        dagsats = null
    )

    companion object {
        fun avslag(virkningsdato: LocalDate) = Vedtak(utfall = false, virkningsdato = virkningsdato)
        fun innvilgelse(virkningsdato: LocalDate, grunnlag: BigDecimal, dagsats: BigDecimal) =
            Vedtak(utfall = true, virkningsdato = virkningsdato, grunnlag = grunnlag, dagsats = dagsats)
    }

    fun accept(visitor: PersonVisitor) {
        visitor.visitVedtak(utfall = utfall, grunnlag = grunnlag, dagsats = dagsats)
    }
}
