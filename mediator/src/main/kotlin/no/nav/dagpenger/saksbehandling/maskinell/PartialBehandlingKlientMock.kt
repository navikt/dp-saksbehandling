package no.nav.dagpenger.saksbehandling.maskinell

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import java.io.FileNotFoundException
import java.util.UUID

class PartialBehandlingKlientMock(private val behandlingKlient: BehandlingKlient) : BehandlingKlient {
    override suspend fun hentBehandling(
        behandlingId: UUID,
        saksbehandlerToken: String,
    ): Pair<BehandlingDTO, Map<String, Any>> {
        return kotlin.runCatching {
            behandlingKlient.hentBehandling(behandlingId, saksbehandlerToken)
        }.getOrDefault(behandlingResponseMock())
    }

    override suspend fun bekreftBehandling(behandlingId: UUID, saksbehandlerToken: String) {
        behandlingKlient.bekreftBehandling(behandlingId, saksbehandlerToken)
    }

    override suspend fun godkjennBehandling(behandlingId: UUID, ident: String, saksbehandlerToken: String): Int {
        TODO("Not yet implemented")
    }

    private fun String.fileAsText(): String {
        return object {}.javaClass.getResource(this)?.readText()
            ?: throw FileNotFoundException()
    }

    private fun behandlingResponseMock(): Pair<BehandlingDTO, Map<String, Any>> {
        val fileAsText = "/behandlingResponseMock.json".fileAsText()
        return Pair(
            objectMapper.readValue<BehandlingDTO>(fileAsText),
            objectMapper.readValue(fileAsText, object : TypeReference<Map<String, Any>>() {}),
        )
    }
}
