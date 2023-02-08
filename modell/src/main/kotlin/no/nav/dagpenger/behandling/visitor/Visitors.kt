package no.nav.dagpenger.behandling.visitor

import no.nav.dagpenger.behandling.NyRettighetsbehandling
import no.nav.dagpenger.behandling.PersonIdentifikator
import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface PersonVisitor : NyRettighetsbehandlingVisitor, VedtakVisitor {
    fun visitPerson(ident: PersonIdentifikator) {}
}
interface VedtakVisitor {
    fun visitVedtak(utfall: Boolean, grunnlag: BigDecimal?, dagsats: BigDecimal?, stønadsperiode: Stønadsperiode?) {}
}

interface NyRettighetsbehandlingVisitor : VilkårsvurderingVisitor, FastsettelseVisitor {
    fun visitNyRettighetsbehandling(
        søknadsId: UUID,
        behandlingsId: UUID,
        tilstand: NyRettighetsbehandling.Tilstand,
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

interface FastsettelseVisitor {

    fun visitGrunnlag(grunnlag: BigDecimal) {}
    fun visitDagsats(dagsats: BigDecimal) {}
    fun visitStønadsperiode(stønadsperiode: Stønadsperiode) {}
}
