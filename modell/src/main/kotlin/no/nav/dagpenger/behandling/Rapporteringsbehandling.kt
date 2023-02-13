package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse.Companion.vurdert
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_15_Forbruk
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.mengde.Tid
import no.nav.dagpenger.behandling.vilkår.TestVilkår
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.erAlleOppfylt
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
    vilkårsvurderinger = listOf(TestVilkår()),
    aktivitetslogg = aktivitetslogg
) {
    override val fastsettelser by lazy {
        listOf(Paragraf_4_15_Forbruk(person = person))
    }

    object VurdererVilkår : Tilstand.VurdererVilkår<Rapporteringsbehandling> () {
        override fun håndter(rapporteringsHendelse: RapporteringsHendelse, behandling: Rapporteringsbehandling) {
            behandling.vilkårsvurderinger.forEach { it.håndter(rapporteringsHendelse) }
            if (behandling.vilkårsvurderinger.vurdert()) {
                behandling.endreTilstand(VurderUtfall, rapporteringsHendelse)
            }
        }
    }

    object VurderUtfall : Tilstand.VurderUtfall<Rapporteringsbehandling>() {
        override fun entering(hendelse: Hendelse, behandling: Rapporteringsbehandling) {
            require(behandling.vilkårsvurderinger.vurdert()) { "Vilkårsvurderinger må være ferdig vurdert på dette tidspunktet" }
            if (behandling.vilkårsvurderinger.erAlleOppfylt()) {
                behandling.endreTilstand(Fastsetter, hendelse)
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

    object Behandlet : Tilstand.Fastsetter<Rapporteringsbehandling>() {
        override fun entering(hendelse: Hendelse, behandling: Rapporteringsbehandling) {
            behandling.opprettVedtak()
        }
    }

    private fun opprettVedtak() {
        person.leggTilVedtak(Vedtak.løpendeVedtak(virkningsdato = LocalDate.now(), forbruk = FastsattForbruk(fastsettelser).forbruk))
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

        lateinit var forbruk: Tid
        init {
            fastsettelser.forEach { it.accept(this) }
        }

        override fun visitForbruk(forbruk: Tid) {
            this.forbruk = forbruk
        }
    }
}
