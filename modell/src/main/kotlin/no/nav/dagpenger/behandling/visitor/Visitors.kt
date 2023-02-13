package no.nav.dagpenger.behandling.visitor

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.PersonIdentifikator
import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface PersonVisitor : NyRettighetsbehandlingVisitor, VedtakVisitor {
    fun visitPerson(ident: PersonIdentifikator) {}
}
interface VedtakVisitor {
    fun preVisitVedtak(vedtakId: UUID, virkningsdato: LocalDate, vedtakstidspunkt: LocalDateTime, utfall: Boolean) {}
    fun visitVedtakGrunnlag(grunnlag: BigDecimal) {}
    fun visitVedtakDagsats(dagsats: BigDecimal) {}
    fun visitVedtakStønadsperiode(stønadsperiode: Stønadsperiode) {}
    fun postVisitVedtak(vedtakId: UUID, virkningsdato: LocalDate, vedtakstidspunkt: LocalDateTime, utfall: Boolean) {}
}

interface BehandlingVisitor {
    fun preVisit(behandlingsId: UUID, hendelseId: UUID) {}
    fun visitTilstand(tilstand: Behandling.Tilstand.Type) {}
    fun postVisit(behandlingsId: UUID, hendelseId: UUID) {}
}

interface NyRettighetsbehandlingVisitor : VilkårsvurderingVisitor, BehandlingVisitor {
    fun visitNyRettighetsbehandling(
        søknadsId: UUID,
        behandlingsId: UUID,
        virkningsdato: LocalDate?,
        inntektsId: String?
    ) {}
}

interface VilkårsvurderingVisitor {
    fun <Paragraf : Vilkårsvurdering<Paragraf>> visitVilkårsvurdering(
        vilkårsvurderingId: UUID,
        tilstand: Vilkårsvurdering.Tilstand<Paragraf>
    ) {}
}

internal interface FastsettelseVisitor {

    fun visitGrunnlag(grunnlag: BigDecimal) {}
    fun visitDagsats(dagsats: BigDecimal) {}
    fun visitStønadsperiode(stønadsperiode: Stønadsperiode) {}
}
