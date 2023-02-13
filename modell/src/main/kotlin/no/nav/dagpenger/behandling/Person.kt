package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Behandling.Companion.harHendelseId
import no.nav.dagpenger.behandling.PersonIdentifikator.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.PersonObserver.VedtakFattet
import no.nav.dagpenger.behandling.hendelser.BeslutterHendelse
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.hendelser.StønadsperiodeResultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.rapportering.Rapporteringsperioder
import no.nav.dagpenger.behandling.visitor.PersonVisitor
import no.nav.dagpenger.behandling.visitor.VedtakVisitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Person private constructor(private val ident: PersonIdentifikator) : Aktivitetskontekst by ident {
    private val behandlinger = mutableListOf<Behandling<*>>()

    private val vedtakHistorikk = VedtakHistorikk()
    private val rapporteringsperioder = Rapporteringsperioder()

    private val observere = mutableListOf<PersonObserver>()

    constructor(ident: String) : this(ident.tilPersonIdentfikator())

    companion object {
        const val kontekstType = "Person"
    }

    fun accept(visitor: PersonVisitor) {
        visitor.visitPerson(ident)
        behandlinger.forEach {
            it.accept(visitor)
        }
        vedtakHistorikk.accept(visitor)
    }

    fun addObserver(observer: PersonObserver) {
        observere.add(observer)
    }

    fun håndter(søknadHendelse: SøknadHendelse) {
        kontekst(søknadHendelse)
        if (behandlinger.harHendelseId(søknadHendelse.søknadUUID())) return
        søknadHendelse.info("Har mottatt ny søknadhendelse")
        val behandling = NyRettighetsbehandling(this, søknadHendelse.søknadUUID())
        behandlinger.add(behandling)
        behandling.håndter(søknadHendelse)
    }

    fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat) {
        kontekst(paragraf423AlderResultat)
        behandlinger.forEach { it.håndter(paragraf423AlderResultat) }
    }
    fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        kontekst(grunnlagOgSatsResultat)
        behandlinger.forEach { it.håndter(grunnlagOgSatsResultat) }
    }
    fun håndter(stønadsperiode: StønadsperiodeResultat) {
        kontekst(stønadsperiode)
        behandlinger.forEach { it.håndter(stønadsperiode) }
    }

    fun håndter(beslutterHendelse: BeslutterHendelse) {
        kontekst(beslutterHendelse)
        behandlinger.forEach { it.håndter(beslutterHendelse) }
    }

    fun håndter(rapporteringsHendelse: RapporteringsHendelse) {
        rapporteringsperioder.håndter(rapporteringsHendelse)
        kontekst(rapporteringsHendelse)
        val behandling = Rapporteringsbehandling(this, rapporteringsHendelse.rapporteringsId)
        behandlinger.add(behandling)
        behandling.håndter(rapporteringsHendelse)
    }

    fun ident() = this.ident.identifikator()

    private fun kontekst(hendelse: Hendelse) {
        hendelse.kontekst(this)
    }

    internal fun leggTilVedtak(vedtak: Vedtak) {
        vedtakHistorikk.leggTilVedtak(vedtak)
        observere.forEach { it.vedtakFattet(VedtakFattetVisitor(this.ident(), vedtak).vedtakFattet) }
    }

    private class VedtakFattetVisitor(val ident: String, val vedtak: Vedtak) : VedtakVisitor {
        lateinit var vedtakFattet: VedtakFattet
        init {
            vedtak.accept(this)
        }

        override fun preVisitVedtak(
            vedtakId: UUID,
            virkningsdato: LocalDate,
            vedtakstidspunkt: LocalDateTime,
            utfall: Boolean
        ) {
            this.vedtakFattet = VedtakFattet(ident, utfall)
        }
    }
}
