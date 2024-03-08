package no.nav.dagpenger.saksbehandling.maskinell

import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import java.io.FileNotFoundException
import java.util.UUID

class PartialBehandlingKlientMock(private val behandlingKlient: BehandlingKlient) : BehandlingKlient {
    override suspend fun hentBehandling(behandlingId: UUID, saksbehandlerToken: String): BehandlingDTO {
        return kotlin.runCatching {
            behandlingKlient.hentBehandling(behandlingId, saksbehandlerToken)
        }.getOrDefault(behandlingResponseMock(behandlingId))
    }

    override suspend fun bekreftBehandling(behandlingId: UUID, saksbehandlerToken: String) {
        behandlingKlient.bekreftBehandling(behandlingId, saksbehandlerToken)
    }

    private fun String.fileAsText(): String {
        return object {}.javaClass.getResource(this)?.readText()
            ?: throw FileNotFoundException()
    }

    private fun behandlingResponseMock(behandlingId: UUID) = templateBehandlingDTO.copy(behandlingId = behandlingId)

    private val templateBehandlingDTO = objectMapper.readValue(
        "/behandlingResponseMock.json".fileAsText(),
        BehandlingDTO::class.java,
    )
}
