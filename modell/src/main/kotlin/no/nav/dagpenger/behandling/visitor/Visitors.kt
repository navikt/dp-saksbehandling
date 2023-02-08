package no.nav.dagpenger.behandling.visitor

import no.nav.dagpenger.behandling.NyRettighetsbehandling
import no.nav.dagpenger.behandling.PersonIdentifikator
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import java.time.LocalDate
import java.util.UUID

interface PersonVisitor : NyRettighetsbehandlingVisitor, VedtakVisitor {
    fun visitPerson(ident: PersonIdentifikator) {}
}
interface VedtakVisitor {
    fun visitVedtak(utfall: Boolean) {}
}

interface NyRettighetsbehandlingVisitor : VilkårsvurderingVisitor {
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
