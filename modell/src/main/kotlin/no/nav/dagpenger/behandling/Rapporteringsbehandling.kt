package no.nav.dagpenger.behandling

import java.util.UUID

class Rapporteringsbehandling(
    private val person: Person,
    private val rapporteringsId: UUID,
    private val behandlingsId: UUID = UUID.randomUUID(),
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Behandling(person = person, behandlingsId = behandlingsId, hendelseId = rapporteringsId, aktivitetslogg) {

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        TODO("Not yet implemented")
    }

    interface Tilstand : Aktivitetskontekst {
        val type: Type

        enum class Type {
            VurdererVilkår,
            VurdererUtfall,
            UtførerBeregning,
            Behandlet
        }
    }
}
