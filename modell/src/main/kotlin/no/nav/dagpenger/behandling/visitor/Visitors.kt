package no.nav.dagpenger.behandling.visitor

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.PersonIdentifikator
import no.nav.dagpenger.behandling.entitet.Arbeidstimer
import no.nav.dagpenger.behandling.entitet.Rettighet
import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.mengde.Tid
import no.nav.dagpenger.behandling.rapportering.Arbeidsdag
import no.nav.dagpenger.behandling.rapportering.Fraværsdag
import no.nav.dagpenger.behandling.rapportering.Helgedag
import no.nav.dagpenger.behandling.rapportering.Rapporteringsperioder
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface PersonVisitor : NyRettighetsbehandlingVisitor, VedtakHistorikkVisitor, DagVisitor {
    fun visitPerson(ident: PersonIdentifikator) {}
    fun preVisitRapporteringsperioder(rapporteringsperioder: Rapporteringsperioder) {}
    fun postVisitRapporteringsperioder(rapporteringsperioder: Rapporteringsperioder) {}
}
interface VedtakVisitor {
    fun preVisitVedtak(vedtakId: UUID, virkningsdato: LocalDate, vedtakstidspunkt: LocalDateTime, utfall: Boolean) {}
    fun visitVedtakGrunnlag(grunnlag: BigDecimal) {}
    fun visitVedtakDagsats(dagsats: BigDecimal) {}
    fun visitVedtakStønadsperiode(stønadsperiode: Stønadsperiode) {}
    fun visitVedtakRettigheter(rettigheter: List<Rettighet>) {}
    fun visitForbruk(forbruk: Tid) {}
    fun postVisitVedtak(vedtakId: UUID, virkningsdato: LocalDate, vedtakstidspunkt: LocalDateTime, utfall: Boolean) {}
    fun visitFastsattArbeidstidPerDag(fastsattArbeidstidPerDag: Arbeidstimer) {}
}

interface VedtakHistorikkVisitor : VedtakVisitor {
    fun visitGjenståendeStønadsperiode(gjenståendePeriode: Stønadsperiode) {}
    fun preVisitVedtak() {}
    fun postVisitVedtak() {}
}

interface BehandlingVisitor : VilkårsvurderingVisitor {
    fun preVisit(behandlingsId: UUID, hendelseId: UUID) {}
    fun visitTilstand(tilstand: Behandling.Tilstand.Type) {}
    fun postVisit(behandlingsId: UUID, hendelseId: UUID) {}
}

interface DagVisitor {
    fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {}
    fun visitHelgedag(helgedag: Helgedag) {}
    fun visitFraværsdag(fraværsdag: Fraværsdag) {}
}

interface NyRettighetsbehandlingVisitor : BehandlingVisitor {
    fun visitNyRettighetsbehandling(
        søknadsId: UUID,
        behandlingsId: UUID,
        virkningsdato: LocalDate?,
        inntektsId: String?,
    ) {}
}

interface VilkårsvurderingVisitor {
    fun <Vilkår : Vilkårsvurdering<Vilkår>> visitVilkårsvurdering(
        vilkårsvurderingId: UUID,
        tilstand: Vilkårsvurdering.Tilstand<Vilkår>,
    ) {}

    fun visitInngangsvilkårOppfylt(virkningsdato: LocalDate) {}
    fun visitInngangvilkårIkkeOppfylt(virkningsdato: LocalDate) {}
    fun visitInngangsvilkårOppfylt(fastsattArbeidstidPerDag: Arbeidstimer) {}
}

internal interface FastsettelseVisitor {

    fun visitGrunnlag(grunnlag: BigDecimal) {}
    fun visitDagsats(dagsats: BigDecimal) {}
    fun visitStønadsperiode(stønadsperiode: Stønadsperiode) {}
    fun visitForbruk(forbruk: Tid) {}
}
