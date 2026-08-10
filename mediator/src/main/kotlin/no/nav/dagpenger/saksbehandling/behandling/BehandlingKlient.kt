package no.nav.dagpenger.saksbehandling.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.skjerming.createHttpClient
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

interface BehandlingKlient {
    suspend fun kreverTotrinnskontroll(
        behandlingId: UUID,
        saksbehandlerToken: String,
    ): Result<Boolean>

    fun sendTilbake(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit>

    fun opprettBehandling(
        opprettBehandlingTypeDTO: OpprettBehandlingTypeDTO,
        saksbehandlerToken: String,
    ): Result<UUID>

    fun godkjenn(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit>

    fun beslutt(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit>
}

internal class BehandlingHttpKlient(
    private val dpBehandlingApiUrl: String,
    private val tokenProvider: (String) -> String,
    private val httpClient: HttpClient = lagBehandlingHttpKlient(),
) : BehandlingKlient {
    companion object {
        fun lagBehandlingHttpKlient(
            engine: HttpClientEngine = CIO.create {},
            registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
            metricsBaseName: String = "dp_saksbehandling_behandling_http_klient",
            timeOut: Duration = 15.seconds,
        ): HttpClient =
            createHttpClient(
                engine = engine,
                metricsBaseName = metricsBaseName,
                prometheusRegistry = registry,
                expectSuccess = false,
            ) {
                install(HttpTimeout) {
                    requestTimeoutMillis = timeOut.inWholeMilliseconds
                }
            }
    }

    override fun godkjenn(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit> = kallBehandling("godkjenn", behandlingId, saksbehandlerToken, ident)

    override fun sendTilbake(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit> = kallBehandling("send-tilbake", behandlingId, saksbehandlerToken, ident)

    override fun beslutt(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
    ): Result<Unit> = kallBehandling("beslutt", behandlingId, saksbehandlerToken, ident)

    override fun opprettBehandling(
        opprettBehandlingTypeDTO: OpprettBehandlingTypeDTO,
        saksbehandlerToken: String,
    ): Result<UUID> =
        runBlocking {
            runCatching {
                httpClient
                    .post("$dpBehandlingApiUrl/person/behandling") {
                        header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(saksbehandlerToken)}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                        setBody(opprettBehandlingTypeDTO.toRequestBody())
                    }.body<BehandlingDTO>()
                    .behandlingId
                    .let { behandlingId ->
                        logger.info {
                            "Behandling av type ${opprettBehandlingTypeDTO.behandlingstype} opprettet. HendelseId: ${opprettBehandlingTypeDTO.hendelseId}. Ny behandling har id: $behandlingId"
                        }
                        UUID.fromString(behandlingId)
                    }
            }
        }.onFailure {
            logger.error(it) { "Kall til dp-behandling for å opprette behandling feilet ${it.message}" }
        }

    override suspend fun kreverTotrinnskontroll(
        behandlingId: UUID,
        saksbehandlerToken: String,
    ): Result<Boolean> =
        runCatching {
            httpClient
                .get(urlString = "$dpBehandlingApiUrl/behandling/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(saksbehandlerToken)}")
                    accept(ContentType.Application.Json)
                }.body<BehandlingDTO>()
                .let { behandlingDTO ->
                    logger.info { "Behandling $behandlingId krever totrinnskontroll: ${behandlingDTO.kreverTotrinnskontroll}" }
                    behandlingDTO.kreverTotrinnskontroll
                }
        }.onFailure { logger.error(it) { "Kall til dp-behandling for å hente kreverTotrinnskontroll feilet ${it.message}" } }

    private fun kallBehandling(
        endepunkt: String,
        behandlingId: UUID,
        saksbehandlerToken: String,
        ident: String,
    ): Result<Unit> {
        val urlString = "$dpBehandlingApiUrl/behandling/$behandlingId/$endepunkt"
        return runBlocking {
            try {
                httpClient
                    .post(urlString = urlString) {
                        header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(saksbehandlerToken)}")
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                        setBody(DpBehandlingIdentRequest(ident))
                    }.let {
                        val statuskode = it.status.value
                        logger.info { "Kall til dp-behandling for $endepunkt returnerte status $statuskode" }
                        when (statuskode) {
                            in 200..299 -> Result.success(Unit)
                            else -> Result.failure(BehandlingException(it.bodyAsText(), it.status.value))
                        }
                    }
            } catch (e: Exception) {
                logger.error { "Feil mot dp-behandling for endepunkt: $urlString med ${e.message}" }
                Result.failure(BehandlingException(e.message, 500))
            }
        }
    }
}

data class BehandlingException(
    val text: String?,
    val status: Int,
) : RuntimeException(
        "Feil ved kall mot dp-behandling: $text, status: $status",
    )

class BehandlingKreverIkkeTotrinnskontrollException(
    message: String,
) : RuntimeException(message)

private data class DpBehandlingIdentRequest(
    val ident: String,
)

sealed interface BehandlingRequestBody

open class NyBehandlingRequest(
    val ident: String,
    val behandlingstype: String,
    val id: String,
    val skjedde: LocalDate,
    val begrunnelse: String,
) : BehandlingRequestBody

/**
 * Request-body for behandlingstype "OmgjøringEtterKlage" i dp-behandling.
 * Merk at "id" her betyr noe annet enn i [NyBehandlingRequest] - det er klagens
 * referanse i kildesystemet (Kabal), ikke en intern hendelseId.
 */
data class NyKlageBehandlingRequest(
    val ident: String,
    val behandlingstype: String,
    val id: String,
    val kildesystem: String,
    val skjedde: LocalDate,
    val begrunnelse: String,
) : BehandlingRequestBody

sealed class OpprettBehandlingTypeDTO(
    val personIdent: String,
    val hendelseDato: LocalDate,
    val hendelseId: String,
    val begrunnelse: String,
) {
    abstract val behandlingstype: String

    open fun toRequestBody(): BehandlingRequestBody =
        NyBehandlingRequest(
            ident = personIdent,
            behandlingstype = behandlingstype,
            id = hendelseId,
            skjedde = hendelseDato,
            begrunnelse = begrunnelse,
        )

    class Manuell(
        ident: String,
        hendelseDato: LocalDate,
        hendelseId: String,
        begrunnelse: String,
    ) : OpprettBehandlingTypeDTO(ident, hendelseDato, hendelseId, begrunnelse) {
        override val behandlingstype = "Manuell"
    }

    class Revurdering(
        ident: String,
        hendelseDato: LocalDate,
        hendelseId: String,
        begrunnelse: String,
    ) : OpprettBehandlingTypeDTO(ident, hendelseDato, hendelseId, begrunnelse) {
        override val behandlingstype = "Revurdering"
    }

    /**
     * Revurdering opprettet på bakgrunn av et klageinstansvedtak (Kabal). Sendes til dp-behandling
     * som behandlingstype "OmgjøringEtterKlage" med kildesystem=Klageinstans og [kabalReferanse]
     * som klagens id i kildesystemet.
     */
    class RevurderingEtterKlage(
        ident: String,
        hendelseDato: LocalDate,
        hendelseId: String,
        begrunnelse: String,
        val kabalReferanse: UUID,
    ) : OpprettBehandlingTypeDTO(ident, hendelseDato, hendelseId, begrunnelse) {
        override val behandlingstype = "OmgjøringEtterKlage"

        override fun toRequestBody() =
            NyKlageBehandlingRequest(
                ident = personIdent,
                behandlingstype = behandlingstype,
                id = kabalReferanse.toString(),
                kildesystem = "Klageinstans",
                skjedde = hendelseDato,
                begrunnelse = begrunnelse,
            )
    }
}

private data class BehandlingDTO(
    val behandlingId: String,
    val kreverTotrinnskontroll: Boolean,
)
