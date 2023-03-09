package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.KvalitetssikringsBehov
import no.nav.dagpenger.behandling.NyRettighetsbehandling.Behandlet
import no.nav.dagpenger.behandling.NyRettighetsbehandling.Fastsetter
import no.nav.dagpenger.behandling.NyRettighetsbehandling.Kvalitetssikrer
import no.nav.dagpenger.behandling.entitet.Timer
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse.Companion.vurdert
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_11_Grunnlag
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_12_Sats
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_15_Stønadsperiode
import no.nav.dagpenger.behandling.hendelser.BeslutterHendelse
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.InngangsvilkårResultat
import no.nav.dagpenger.behandling.hendelser.StønadsperiodeResultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.vilkår.Inngangsvilkår
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.oppfylt
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.vurdert
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor
import no.nav.dagpenger.behandling.visitor.NyRettighetsbehandlingVisitor
import no.nav.dagpenger.behandling.visitor.VilkårsvurderingVisitor
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class NyRettighetsbehandling private constructor(
    private val person: Person,
    private val søknadsId: UUID,
    private val behandlingsId: UUID,
    private var virkningsdato: LocalDate?,
    private var fastsattArbeidstidPerDag: Timer?,
    tilstand: Tilstand<NyRettighetsbehandling>,
    private var inntektsId: String?,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Behandling<NyRettighetsbehandling>(
    person = person,
    behandlingsId = behandlingsId,
    hendelseId = søknadsId,
    tilstand = tilstand,
    vilkårsvurdering = Inngangsvilkår(),
    aktivitetslogg,
) {

    constructor(person: Person, søknadUUID: UUID) : this(
        person = person,
        søknadsId = søknadUUID,
        behandlingsId = UUID.randomUUID(),
        tilstand = VurdererVilkår,
        virkningsdato = null,
        fastsattArbeidstidPerDag = null,
        inntektsId = null,
    )

    override val fastsettelser by lazy {
        listOf(
            Paragraf_4_11_Grunnlag(
                requireNotNull(this.inntektsId),
                requireNotNull(this.virkningsdato),
            ),
            Paragraf_4_12_Sats(
                requireNotNull(this.inntektsId),
                requireNotNull(this.virkningsdato),
            ),
            Paragraf_4_15_Stønadsperiode(
                requireNotNull(this.inntektsId),
                requireNotNull(this.virkningsdato),
            ),
        )
    }

    fun håndter(hendelse: SøknadHendelse) {
        kontekst(hendelse, "Opprettet ny rettighetsbehandling basert på søknadhendelse")
        tilstand.håndter(hendelse, this)
    }

    override fun håndter(inngangsvilkårResultat: InngangsvilkårResultat) {
        kontekst(inngangsvilkårResultat, "Fått resultat på ${inngangsvilkårResultat.javaClass.simpleName}")
        tilstand.håndter(inngangsvilkårResultat, this)
    }

    override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        if (grunnlagOgSatsResultat.behandlingsId != this.behandlingsId) return
        kontekst(grunnlagOgSatsResultat, "Fått resultat på ${grunnlagOgSatsResultat.javaClass.simpleName}")
        tilstand.håndter(grunnlagOgSatsResultat, this)
    }

    override fun håndter(stønadsperiode: StønadsperiodeResultat) {
        if (stønadsperiode.behandlingsId != this.behandlingsId) return
        kontekst(stønadsperiode, "Fått resultat på ${stønadsperiode.javaClass.simpleName}")
        tilstand.håndter(stønadsperiode, this)
    }

    override fun håndter(beslutterHendelse: BeslutterHendelse) {
        if (beslutterHendelse.behandlingsId != this.behandlingsId) return
        kontekst(beslutterHendelse, "Beslutter har fattet et vedtak")
        tilstand.håndter(beslutterHendelse, this)
    }

    override fun <T> implementasjon(block: NyRettighetsbehandling.() -> T): T = this.block()

    fun accept(visitor: NyRettighetsbehandlingVisitor) {
        visitor.visitNyRettighetsbehandling(søknadsId, behandlingsId, virkningsdato, inntektsId)
    }

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst(
            kontekstType = kontekstType,
            mapOf(
                "behandlingsId" to behandlingsId.toString(),
                "type" to this.javaClass.simpleName,
                "søknad_uuid" to søknadsId.toString(),
            ),
        )

    object VurdererVilkår : Tilstand.VurdererVilkår<NyRettighetsbehandling>() {

        override fun håndter(søknadHendelse: SøknadHendelse, behandling: NyRettighetsbehandling) {
            behandling.vilkårsvurdering.håndter(søknadHendelse)
        }

        override fun håndter(
            inngangsvilkårResultat: InngangsvilkårResultat,
            behandling: NyRettighetsbehandling,
        ) {
            behandling.vilkårsvurdering.håndter(inngangsvilkårResultat)
            if (behandling.vilkårsvurdering.vurdert()) {
                behandling.endreTilstand(VurdererUtfall, inngangsvilkårResultat)
            }
        }
    }

    object VurdererUtfall : Tilstand.VurderUtfall<NyRettighetsbehandling>() {

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            behandling.virkningsdato = VirkningsdatoVisitor(behandling.vilkårsvurdering).virkningsdato
            require(behandling.vilkårsvurdering.vurdert()) { "Vilkårsvurderinger må være ferdig vurdert på dette tidspunktet" }
            if (behandling.vilkårsvurdering.oppfylt()) {
                behandling.endreTilstand(Fastsetter, hendelse)
            } else {
                behandling.endreTilstand(Kvalitetssikrer, hendelse)
            }
        }
    }

    object Fastsetter : Tilstand.Fastsetter<NyRettighetsbehandling>() {

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            behandling.inntektsId = "ULID"
            behandling.fastsattArbeidstidPerDag = InngangsvilkårOppfyltVisitor(behandling.vilkårsvurdering).fastsattArbeidstidPerDag
            behandling.fastsettelser.forEach { it.håndter(hendelse) }
        }

        override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, behandling: NyRettighetsbehandling) {
            behandling.fastsettelser.forEach { it.håndter(grunnlagOgSatsResultat) }
            if (behandling.fastsettelser.vurdert()) {
                behandling.endreTilstand(Kvalitetssikrer, grunnlagOgSatsResultat)
            }
        }

        override fun håndter(dagpengeperiode: StønadsperiodeResultat, behandling: NyRettighetsbehandling) {
            behandling.fastsettelser.forEach { it.håndter(dagpengeperiode) }
            if (behandling.fastsettelser.vurdert()) {
                behandling.endreTilstand(Kvalitetssikrer, dagpengeperiode)
            }
        }
    }

    object Kvalitetssikrer : Tilstand.Kvalitetssikrer<NyRettighetsbehandling>() {

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            hendelse.behov(
                KvalitetssikringsBehov,
                "Behøver kvalitetssikring i form av totrinnskontroll fra en beslutter",
            )
        }

        override fun håndter(beslutterHendelse: BeslutterHendelse, behandling: NyRettighetsbehandling) {
            behandling.endreTilstand(Behandlet, beslutterHendelse)
        }
    }

    object Behandlet : Tilstand.Behandlet<NyRettighetsbehandling>() {

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            behandling.opprettVedtak()
        }
    }

    private fun opprettVedtak() {
        require(vilkårsvurdering.vurdert()) { " Alle vilkår må være vurdert når en skal opprette vedtak" }
        val vedtak = when (vilkårsvurdering.oppfylt()) {
            true -> {
                val visitor = VedtakFastsettelseVisitor(fastsettelser)
                Vedtak.innvilgelse(
                    virkningsdato = requireNotNull(virkningsdato),
                    grunnlag = visitor.grunnlag,
                    dagsats = visitor.dagsats,
                    stønadsperiode = visitor.stønadsperiode,
                    dagpengerettighet = Dagpengerettighet.OrdinæreDagpenger,
                    fastsattArbeidstidPerDag = requireNotNull(fastsattArbeidstidPerDag),
                    gyldigTom = virkningsdato!!.plusWeeks(5), // TODO: Noe mer fornuftig setting av tom dato
                )
            }

            false -> Vedtak.avslag(requireNotNull(virkningsdato))
        }
        this.person.leggTilVedtak(vedtak)
    }

    private class VedtakFastsettelseVisitor(fastsettelser: List<Fastsettelse<*>>) : FastsettelseVisitor {
        lateinit var grunnlag: BigDecimal
        lateinit var dagsats: BigDecimal
        lateinit var stønadsperiode: Stønadsperiode

        init {
            fastsettelser.forEach { it.accept(this) }
        }

        override fun visitGrunnlag(grunnlag: BigDecimal) {
            this.grunnlag = grunnlag
        }

        override fun visitDagsats(dagsats: BigDecimal) {
            this.dagsats = dagsats
        }

        override fun visitStønadsperiode(stønadsperiode: Stønadsperiode) {
            this.stønadsperiode = stønadsperiode
        }
    }
}

private class VirkningsdatoVisitor(vilkårsvurderinger: Vilkårsvurdering<*>) : VilkårsvurderingVisitor {

    lateinit var virkningsdato: LocalDate

    init {
        vilkårsvurderinger.accept(this)
    }

    override fun visitInngangvilkårIkkeOppfylt(virkningsdato: LocalDate) {
        this.virkningsdato = virkningsdato
    }

    override fun visitInngangsvilkårOppfylt(virkningsdato: LocalDate) {
        this.virkningsdato = virkningsdato
    }
}

private class InngangsvilkårOppfyltVisitor(vilkårsvurderinger: Vilkårsvurdering<*>) : VilkårsvurderingVisitor {

    lateinit var fastsattArbeidstidPerDag: Timer

    init {
        vilkårsvurderinger.accept(this)
    }

    override fun visitInngangsvilkårOppfylt(fastsattArbeidstidPerDag: Timer) {
        this.fastsattArbeidstidPerDag = fastsattArbeidstidPerDag
    }
}
