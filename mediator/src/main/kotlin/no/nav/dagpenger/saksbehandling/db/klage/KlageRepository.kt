package no.nav.dagpenger.saksbehandling.db.klage

import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import java.util.UUID

interface KlageRepository {
    fun hentKlageBehandling(behandlingId: UUID): KlageBehandling

    fun lagre(klageBehandling: KlageBehandling)

    class KlageBehandlingIkkeFunnet(message: String) : RuntimeException(message)
}

object InmemoryKlageRepository : KlageRepository {
    val testKlageId1 = UUID.fromString("01905da1-32bc-7f57-83df-61dcd3a20ea6")

    private val klageBehandlinger =
        mutableMapOf<UUID, KlageBehandling>().also {
            it[testKlageId1] =
                KlageBehandling(
                    behandlingId = testKlageId1,
                )
        }

    override fun hentKlageBehandling(behandlingId: UUID): KlageBehandling {
        return klageBehandlinger[behandlingId]
            ?: throw KlageRepository.KlageBehandlingIkkeFunnet("Fant ikke klagebehandling med id $behandlingId")
    }

    override fun lagre(klageBehandling: KlageBehandling) {
        klageBehandlinger[klageBehandling.behandlingId] = klageBehandling
    }
}
