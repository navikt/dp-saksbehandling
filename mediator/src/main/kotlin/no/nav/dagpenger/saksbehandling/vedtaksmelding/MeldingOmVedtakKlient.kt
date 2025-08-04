package no.nav.dagpenger.saksbehandling.vedtaksmelding
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.prometheus.metrics.model.registry.PrometheusRegistry
import mu.KotlinLogging
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import java.util.UUID

private val logger = KotlinLogging.logger {}

class MeldingOmVedtakKlient(
    private val dpMeldingOmVedtakUrl: String,
    private val tokenProvider: (String) -> String,
    private val httpClient: HttpClient = lagMeldingOmVedtakKlient(),
) {
    companion object {
        fun lagMeldingOmVedtakKlient(
            engine: HttpClientEngine = CIO.create {},
            registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
            metricsBaseName: String = "dp_saksbehandling_melding_om_vedtak_httpklient",
        ): HttpClient {
            return HttpClient(engine = engine) {
                expectSuccess = true
                install(PrometheusMetricsPlugin) {
                    baseName = metricsBaseName
                    this.registry = registry
                }
            }
        }
    }

    suspend fun lagOgHentMeldingOmVedtak(
        person: PDLPersonIntern,
        saksbehandler: BehandlerDTO,
        beslutter: BehandlerDTO?,
        behandlingId: UUID,
        saksbehandlerToken: String,
        behandlingType: BehandlingType = BehandlingType.RETT_TIL_DAGPENGER,
        sakId: String? = null,
    ): Result<String> {
        val meldingOmVedtakDataDTO =
            MeldingOmVedtakDataDTO(
                fornavn = person.fornavn,
                etternavn = person.etternavn,
                fodselsnummer = person.ident,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                behandlingstype = behandlingType.name,
                sakId = sakId,
            )
        return kotlin.runCatching {
            httpClient.post("$dpMeldingOmVedtakUrl/melding-om-vedtak/$behandlingId/vedtaksmelding") {
                header("Authorization", "Bearer ${tokenProvider.invoke(saksbehandlerToken)}")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(objectMapper.writeValueAsString(meldingOmVedtakDataDTO))
            }.bodyAsText()
        }.onFailure {
            logger.error(it) { "Feil ved henting av melding om vedtak for behandlingId: $behandlingId" }
            throw KanIkkeLageMeldingOmVedtak("Kan ikke lage melding om vedtak for behandlingId: $behandlingId")
        }
    }

    suspend fun lagOgHentMeldingOmVedtakM2M(
        person: PDLPersonIntern,
        saksbehandler: BehandlerDTO,
        beslutter: BehandlerDTO?,
        behandlingId: UUID,
        maskinToken: String,
        behandlingType: BehandlingType = BehandlingType.RETT_TIL_DAGPENGER,
        sakId: String? = null,
    ): Result<String> {
        val meldingOmVedtakDataDTO =
            MeldingOmVedtakDataDTO(
                fornavn = person.fornavn,
                etternavn = person.etternavn,
                fodselsnummer = person.ident,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                behandlingstype = behandlingType.name,
                sakId = sakId,
            )
        return kotlin.runCatching {
            httpClient.post("$dpMeldingOmVedtakUrl/melding-om-vedtak/$behandlingId/vedtaksmelding") {
                header("Authorization", "Bearer $maskinToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(objectMapper.writeValueAsString(meldingOmVedtakDataDTO))
            }.bodyAsText()
        }.onFailure {
            logger.error(it) { "Feil ved henting av melding om vedtak for behandlingId: $behandlingId" }
            throw KanIkkeLageMeldingOmVedtak("Kan ikke lage melding om vedtak for behandlingId: $behandlingId")
        }
    }

    class KanIkkeLageMeldingOmVedtak(message: String) : RuntimeException(message)
}

private data class MeldingOmVedtakDataDTO(
    val fornavn: String,
    val etternavn: String,
    val fodselsnummer: String,
    val saksbehandler: BehandlerDTO,
    val mellomnavn: String? = null,
    val beslutter: BehandlerDTO? = null,
    val behandlingstype: String,
    val sakId: String? = null,
)
