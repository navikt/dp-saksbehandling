package no.nav.dagpenger.behandling

import java.util.UUID

class Rapporteringsbehandling(
    private val person: Person,
    private val rapporteringsId: UUID,
    private val behandlingsId: UUID = UUID.randomUUID(),
    tilstand: Behandling.Tilstand<Rapporteringsbehandling> = VurdererVilkår,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Behandling<Rapporteringsbehandling>(person = person, behandlingsId = behandlingsId, hendelseId = rapporteringsId, tilstand = tilstand, aktivitetslogg) {

    object VurdererVilkår : Tilstand.VurdererVilkår<Rapporteringsbehandling> ()

    override fun <T> implementasjon(block: Rapporteringsbehandling.() -> T): T = this.block()

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        TODO("Not yet implemented")
    }
}
