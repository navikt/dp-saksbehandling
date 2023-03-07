package no.nav.dagpenger.behandling.rapportering

import no.nav.dagpenger.behandling.Aktivitetslogg
import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.SpesifikkKontekst
import no.nav.dagpenger.behandling.Vedtak
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse.Companion.vurdert
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_15_Forbruk
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Rapporteringshendelse
import no.nav.dagpenger.behandling.mengde.Enhet.Companion.arbeidsdager
import no.nav.dagpenger.behandling.mengde.Tid
import no.nav.dagpenger.behandling.rapportering.Rapporteringsbehandling.Behandlet
import no.nav.dagpenger.behandling.rapportering.Rapporteringsbehandling.Fastsetter
import no.nav.dagpenger.behandling.rapportering.Rapporteringsbehandling.VurderUtfall
import no.nav.dagpenger.behandling.rapportering.Rapporteringsbehandling.VurdererVilkår
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
    private val tellendeDager: MutableList<Dag> = mutableListOf(),
    tilstand: Tilstand<Rapporteringsbehandling> = ForberedendeFakta,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Behandling<Rapporteringsbehandling>(
    person = person,
    behandlingsId = behandlingsId,
    hendelseId = rapporteringsId,
    tilstand = tilstand,
    vilkårsvurdering = LøpendeStønadsperiodeVilkår(person),
    aktivitetslogg = aktivitetslogg,
) {
    override val fastsettelser by lazy {
        listOf(Paragraf_4_15_Forbruk(person = person))
    }

    object ForberedendeFakta : Tilstand.ForberedBehandling<Rapporteringsbehandling>() {
        override fun håndter(rapporteringsHendelse: Rapporteringshendelse, behandling: Rapporteringsbehandling) {
            behandling.tellendeDager.addAll(
                TellendeDager(behandling.person, rapporteringsHendelse.somPeriode()).tellendeDager(),
            )
            behandling.endreTilstand(VurdererVilkår, rapporteringsHendelse)
        }
    }

    object VurdererVilkår : Tilstand.VurdererVilkår<Rapporteringsbehandling>() {
        override fun entering(hendelse: Hendelse, behandling: Rapporteringsbehandling) {
            require(hendelse is Rapporteringshendelse) { "Hendelse er ikke rapporteringshendelse. Hendelsetype: ${hendelse.javaClass.simpleName}. Tilstand: $type" }
            behandling.vilkårsvurdering.håndter(hendelse, behandling.tellendeDager)
            if (behandling.vilkårsvurdering.vurdert()) {
                behandling.endreTilstand(VurderUtfall, hendelse)
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
            if (hendelse is Rapporteringshendelse) {
                behandling.fastsettelser.forEach { it.håndter(hendelse, behandling.tellendeDager) }
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
                vilkårsvurdering.oppfylt(),
            ),
        )
    }

    override fun <T> implementasjon(block: Rapporteringsbehandling.() -> T): T = this.block()

    override fun toSpesifikkKontekst(): SpesifikkKontekst = SpesifikkKontekst(
        kontekstType = kontekstType,
        mapOf(
            "behandlingsId" to behandlingsId.toString(),
            "type" to this.javaClass.simpleName,
            "hendelse_uuid" to hendelseId.toString(),
        ),
    )

    fun håndter(rapporteringsHendelse: Rapporteringshendelse) {
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
