package no.nav.dagpenger.behandling.visitor

import no.nav.dagpenger.behandling.AktivitetsloggVisitor
import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Dagpengerettighet
import no.nav.dagpenger.behandling.PersonIdentifikator
import no.nav.dagpenger.behandling.entitet.Timer
import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.mengde.Tid
import no.nav.dagpenger.behandling.rapportering.Arbeidsdag
import no.nav.dagpenger.behandling.rapportering.Fraværsdag
import no.nav.dagpenger.behandling.rapportering.Helgedag
import no.nav.dagpenger.behandling.rapportering.Rapporteringsperiode
import no.nav.dagpenger.behandling.rapportering.Rapporteringsperioder
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface PersonVisitor : NyRettighetsbehandlingVisitor, VedtakHistorikkVisitor, RapporteringsperiodeVisitor {
    fun visitPerson(ident: PersonIdentifikator) {}
    fun preVisitRapporteringsperioder(rapporteringsperioder: Rapporteringsperioder) {}
    fun postVisitRapporteringsperioder(rapporteringsperioder: Rapporteringsperioder) {}
}
interface VedtakVisitor {
    fun preVisitVedtak(vedtakId: UUID, virkningsdato: LocalDate, vedtakstidspunkt: LocalDateTime, utfall: Boolean) {}
    fun visitRammeVedtak(
        grunnlag: BigDecimal,
        dagsats: BigDecimal,
        stønadsperiode: Stønadsperiode,
        fastsattArbeidstidPerDag: Timer,
        dagpengerettighet: Dagpengerettighet,
        gyldigTom: LocalDate?,
    ) {}

    @Deprecated("Bruk visitRammeVedtak")
    fun visitVedtakGrunnlag(grunnlag: BigDecimal) {}

    @Deprecated("Bruk visitRammeVedtak")
    fun visitVedtakDagsats(dagsats: BigDecimal) {}

    @Deprecated("Bruk visitRammeVedtak")
    fun visitVedtakStønadsperiode(stønadsperiode: Stønadsperiode) {}

    @Deprecated("Bruk visitRammeVedtak")
    fun visitVedtakDagpengerettighet(dagpengerettighet: Dagpengerettighet) {}

    @Deprecated("Bruk visitRammeVedtak")
    fun visitFastsattArbeidstidPerDag(fastsattArbeidstidPerDag: Timer) {}

    fun visitForbruk(forbruk: Tid) {}

    fun postVisitVedtak(
        vedtakId: UUID,
        virkningsdato: LocalDate,
        vedtakstidspunkt: LocalDateTime,
        utfall: Boolean,
        gyldigTom: LocalDate?,
    ) {}
}

interface VedtakHistorikkVisitor : VedtakVisitor {
    fun visitGjenståendeStønadsperiode(gjenståendePeriode: Stønadsperiode) {}
    fun preVisitVedtak() {}
    fun postVisitVedtak() {}
}

interface BehandlingVisitor : VilkårsvurderingVisitor, AktivitetsloggVisitor {
    fun preVisit(behandling: Behandling<*>, behandlingsId: UUID, hendelseId: UUID) {}
    fun visitTilstand(tilstand: Behandling.Tilstand.Type) {}
    fun postVisit(behandling: Behandling<*>, behandlingsId: UUID, hendelseId: UUID) {}
}

interface RapporteringsperiodeVisitor {
    fun preVisitRapporteringPeriode(rapporteringsperiode: Rapporteringsperiode) {}
    fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {}
    fun visitHelgedag(helgedag: Helgedag) {}
    fun visitFraværsdag(fraværsdag: Fraværsdag) {}
    fun postVisitRapporteringPeriode(rapporteringsperiode: Rapporteringsperiode) {}
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
    fun visitInngangsvilkårOppfylt(fastsattArbeidstidPerDag: Timer) {}
}

internal interface FastsettelseVisitor {

    fun visitGrunnlag(grunnlag: BigDecimal) {}
    fun visitDagsats(dagsats: BigDecimal) {}
    fun visitStønadsperiode(stønadsperiode: Stønadsperiode) {}
    fun visitForbruk(forbruk: Tid) {}
}
