package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Rapporteringsbehandling.Behandlet
import no.nav.dagpenger.behandling.Rapporteringsbehandling.Fastsetter
import no.nav.dagpenger.behandling.Rapporteringsbehandling.VurderUtfall
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse.Companion.vurdert
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_15_Forbruk
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.mengde.Enhet.Companion.arbeidsdager
import no.nav.dagpenger.behandling.mengde.Tid
import no.nav.dagpenger.behandling.vilkår.LøpendeStønadsperiodeVilkår
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.oppfylt
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.vurdert
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor
import java.time.LocalDate
import java.util.UUID

class Rapporteringsbehandling(
    private val person: Person,
    private val rapporteringsId: UUID,
    private val behandlingsId: UUID = UUID.randomUUID(),
    tilstand: Tilstand<Rapporteringsbehandling> = VurdererVilkår,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Behandling<Rapporteringsbehandling>(
    person = person,
    behandlingsId = behandlingsId,
    hendelseId = rapporteringsId,
    tilstand = tilstand,
    vilkårsvurdering = LøpendeStønadsperiodeVilkår(person),
    aktivitetslogg = aktivitetslogg
) {
    override val fastsettelser by lazy {
        listOf(Paragraf_4_15_Forbruk(person = person))
    }

    object VurdererVilkår : Tilstand.VurdererVilkår<Rapporteringsbehandling>() {
        override fun håndter(rapporteringsHendelse: RapporteringsHendelse, behandling: Rapporteringsbehandling) {
            behandling.vilkårsvurdering.håndter(rapporteringsHendelse)
            if (behandling.vilkårsvurdering.vurdert()) {
                behandling.endreTilstand(VurderUtfall, rapporteringsHendelse)
            }
        }
    }

    object VurderUtfall : Tilstand.VurderUtfall<Rapporteringsbehandling>() {
        override fun entering(hendelse: Hendelse, behandling: Rapporteringsbehandling) {
            require(behandling.vilkårsvurdering.vurdert()) { "Vilkårsvurderinger må være ferdig vurdert på dette tidspunktet" }
            if (behandling.vilkårsvurdering.oppfylt()) {
                behandling.endreTilstand(Fastsetter, hendelse)
            } else {
                behandling.endreTilstand(Behandlet, hendelse)
            }
        }
    }

    object Fastsetter : Tilstand.Fastsetter<Rapporteringsbehandling>() {

        override fun entering(hendelse: Hendelse, behandling: Rapporteringsbehandling) {
            if (hendelse is RapporteringsHendelse) {
                behandling.fastsettelser.forEach { it.håndter(hendelse) }
                if (behandling.fastsettelser.vurdert()) {
                    behandling.endreTilstand(Behandlet, hendelse)
                }
            }
        }
    }

    object Behandlet : Tilstand.Behandlet<Rapporteringsbehandling>() {
        override fun entering(hendelse: Hendelse, behandling: Rapporteringsbehandling) {
            behandling.opprettVedtak()
        }
    }

    private fun opprettVedtak() {
        person.leggTilVedtak(
            Vedtak.løpendeVedtak(
                virkningsdato = LocalDate.now(),
                forbruk = FastsattForbruk(fastsettelser).forbruk,
                vilkårsvurdering.oppfylt()
            )
        )
    }

    override fun <T> implementasjon(block: Rapporteringsbehandling.() -> T): T = this.block()

    override fun toSpesifikkKontekst(): SpesifikkKontekst = SpesifikkKontekst(
        kontekstType = kontekstType,
        mapOf(
            "behandlingsId" to behandlingsId.toString(),
            "type" to this.javaClass.simpleName,
            "hendelse_uuid" to hendelseId.toString()
        )
    )

    fun håndter(rapporteringsHendelse: RapporteringsHendelse) {
        kontekst(rapporteringsHendelse, "Opprettet ny rapporteringsbehandling basert på rapporteringshendelse")
        tilstand.håndter(rapporteringsHendelse, this)
    }

    private class FastsattForbruk(fastsettelser: List<Fastsettelse<*>>) : FastsettelseVisitor {

        var forbruk: Tid = 0.arbeidsdager

        init {
            fastsettelser.forEach { it.accept(this) }
        }

        override fun visitForbruk(forbruk: Tid) {
            this.forbruk += forbruk
        }
    }
}
