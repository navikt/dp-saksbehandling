package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.KvalitetssikringsBehov
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse.Companion.vurdert
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_11_Grunnlag
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_12_Sats
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_15_Stønadsperiode
import no.nav.dagpenger.behandling.hendelser.BeslutterHendelse
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.StønadsperiodeResultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.vilkår.Paragraf_4_23_alder_vilkår
import no.nav.dagpenger.behandling.vilkår.TestVilkår
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.erAlleOppfylt
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.vurdert
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor
import no.nav.dagpenger.behandling.visitor.NyRettighetsbehandlingVisitor
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class NyRettighetsbehandling private constructor(
    private val person: Person,
    private val søknadsId: UUID,
    private val behandlingsId: UUID,
    private var virkningsdato: LocalDate?,
    tilstand: Tilstand<NyRettighetsbehandling>,
    private var inntektsId: String?,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Behandling<NyRettighetsbehandling>(
    person = person,
    behandlingsId = behandlingsId,
    hendelseId = søknadsId,
    tilstand = tilstand,
    vilkårsvurderinger = listOf(
        Paragraf_4_23_alder_vilkår(),
        TestVilkår()
    ),
    aktivitetslogg
) {

    constructor(person: Person, søknadUUID: UUID) : this(
        person = person,
        søknadsId = søknadUUID,
        behandlingsId = UUID.randomUUID(),
        tilstand = VurdererVilkår,
        virkningsdato = null,
        inntektsId = null
    )

    override val fastsettelser by lazy {
        listOf(
            Paragraf_4_11_Grunnlag(
                requireNotNull(this.inntektsId),
                requireNotNull(this.virkningsdato)
            ),
            Paragraf_4_12_Sats(
                requireNotNull(this.inntektsId),
                requireNotNull(this.virkningsdato)
            ),
            Paragraf_4_15_Stønadsperiode(
                requireNotNull(this.inntektsId),
                requireNotNull(this.virkningsdato)
            )
        )
    }

    fun håndter(hendelse: SøknadHendelse) {
        kontekst(hendelse, "Opprettet ny rettighetsbehandling basert på søknadhendelse")
        tilstand.håndter(hendelse, this)
    }

    override fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat) {
        kontekst(paragraf423AlderResultat, "Fått resultat på ${paragraf423AlderResultat.javaClass.simpleName}")
        tilstand.håndter(paragraf423AlderResultat, this)
    }

    override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        if (grunnlagOgSatsResultat.behandlingsId != this.behandlingsId) return
        kontekst(grunnlagOgSatsResultat, "Fått resultat på ${grunnlagOgSatsResultat.javaClass.simpleName}")
        tilstand.håndter(grunnlagOgSatsResultat, this)
    }

    override fun håndter(dagpengeperiode: StønadsperiodeResultat) {
        if (dagpengeperiode.behandlingsId != this.behandlingsId) return
        kontekst(dagpengeperiode, "Fått resultat på ${dagpengeperiode.javaClass.simpleName}")
        tilstand.håndter(dagpengeperiode, this)
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
                "søknad_uuid" to søknadsId.toString()
            )
        )

    object VurdererVilkår : Tilstand.VurdererVilkår<NyRettighetsbehandling>() {

        override fun håndter(søknadHendelse: SøknadHendelse, behandling: NyRettighetsbehandling) {
            behandling.vilkårsvurderinger.forEach { vurdering ->
                vurdering.håndter(søknadHendelse)
            }
        }

        override fun håndter(
            paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat,
            behandling: NyRettighetsbehandling,
        ) {
            behandling.vilkårsvurderinger.forEach { vurdering ->
                vurdering.håndter(paragraf423AlderResultat)
            }
            if (behandling.vilkårsvurderinger.vurdert()) {
                behandling.endreTilstand(VurdererUtfall, paragraf423AlderResultat)
            }
        }
    }

    object VurdererUtfall : Tilstand.VurderUtfall<NyRettighetsbehandling>() {

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            behandling.virkningsdato = LocalDate.now() // Må være satt i vilkårsvurderinger
            require(behandling.vilkårsvurderinger.vurdert()) { "Vilkårsvurderinger må være ferdig vurdert på dette tidspunktet" }
            if (behandling.vilkårsvurderinger.erAlleOppfylt()) {
                behandling.endreTilstand(Fastsetter, hendelse)
            } else {
                behandling.endreTilstand(Kvalitetssikrer, hendelse)
            }
        }
    }

    object Fastsetter : Tilstand.Fastsetter<NyRettighetsbehandling>() {

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            behandling.inntektsId = "ULID"
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
            hendelse.behov(KvalitetssikringsBehov, "Behøver kvalitetssikring i form av totrinnskontroll fra en beslutter")
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
        require(vilkårsvurderinger.vurdert()) { " Alle vilkår må være vurdert når en skal opprette vedtak" }
        val vedtak = when (vilkårsvurderinger.erAlleOppfylt()) {
            true -> {
                val visitor = VedtakFastsettelseVisitor(fastsettelser)
                Vedtak.innvilgelse(
                    requireNotNull(virkningsdato),
                    grunnlag = visitor.grunnlag,
                    dagsats = visitor.dagsats,
                    stønadsperiode = visitor.stønadsperiode
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
