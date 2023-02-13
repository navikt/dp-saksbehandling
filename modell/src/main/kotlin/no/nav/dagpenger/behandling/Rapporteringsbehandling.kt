package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.vilkår.TestVilkår
import java.util.UUID

class Rapporteringsbehandling(
    private val person: Person,
    private val rapporteringsId: UUID,
    private val behandlingsId: UUID = UUID.randomUUID(),
    tilstand: Behandling.Tilstand<Rapporteringsbehandling> = VurdererVilkår,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Behandling<Rapporteringsbehandling>(
    person = person,
    behandlingsId = behandlingsId,
    hendelseId = rapporteringsId,
    tilstand = tilstand,
    vilkårsvurderinger = listOf(TestVilkår()),
    aktivitetslogg = aktivitetslogg
) {

    object VurdererVilkår : Tilstand.VurdererVilkår<Rapporteringsbehandling> () {
        override fun håndter(rapporteringsHendelse: RapporteringsHendelse, behandling: Rapporteringsbehandling) {
            behandling.vilkårsvurderinger.forEach { it.håndter(rapporteringsHendelse) }
        }
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
}
