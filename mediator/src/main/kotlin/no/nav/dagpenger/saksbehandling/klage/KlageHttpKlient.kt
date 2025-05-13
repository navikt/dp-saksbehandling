package no.nav.dagpenger.saksbehandling.klage

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import mu.KotlinLogging
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin

private val logger = KotlinLogging.logger {}

class KlageHttpKlient(
    private val kabalApiUrl: String,
    private val tokenProvider: () -> String,
    private val httpClient: HttpClient = httpClient(),
) : KlageKlient {
    override suspend fun registrerKlage(
        klageBehandling: KlageBehandling,
        personIdentId: String,
        fagsakId: String,
        forrigeBehandlendeEnhet: String,
        tilknyttedeJournalposter: List<Journalposter>,
    ): Result<HttpStatusCode> {
        return kotlin.runCatching {
            httpClient.post(urlString = "$kabalApiUrl/api/oversendelse/v4/sak") {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(
                    KlageinstansOversendelse(
                        sakenGjelder =
                            PersonIdent(
                                id =
                                    PersonIdentId(
                                        verdi = personIdentId,
                                    ),
                            ),
                        fagsak =
                            Fagsak(
                                fagsakId = fagsakId,
                                // TODO: https://github.com/navikt/klage-kodeverk/blob/main/src/main/kotlin/no/nav/klage/kodeverk/Fagsystem.kt
                                fagsystem = "DAGPENGER?",
                            ),
                        kildeReferanse = klageBehandling.behandlingId.toString(),
                        forrigeBehandlendeEnhet = forrigeBehandlendeEnhet,
                        tilknyttedeJournalposter = listOf(),
                        hjemler = klageBehandling.hjemler(),
                    ),
                )
            }.status
        }.onFailure { throwable ->
            logger.error(throwable) { "Kall til kabal api feilet for klagebehandling med id: ${klageBehandling.behandlingId}" }
        }
    }
}

internal fun KlageBehandling.hjemler(): List<Hjemler> {
    val verdi =
        this.synligeOpplysninger()
            .singleOrNull { it.type == OpplysningType.HJEMLER }?.verdi() as Verdi.Flervalg?
    return verdi?.value?.map { Hjemler.valueOf(it) }.orEmpty()
}

private data class KlageinstansOversendelse(
    val type: String = "KLAGE",
    val sakenGjelder: PersonIdent,
    val klager: PersonIdent? = null,
    val prosessFullmektig: ProsessFullmektig? = null,
    val fagsak: Fagsak,
    val kildeReferanse: String,
    val dvhReferanse: String? = null,
    val hjemler: List<Hjemler>,
    val forrigeBehandlendeEnhet: String,
    val tilknyttedeJournalposter: List<Journalposter>,
    val brukersKlageMottattVedtaksinstans: String? = null,
    val frist: LocalDate? = null,
    val sakMottattKaTidspunkt: LocalDateTime? = null,
    val ytelse: String = "DAG_DAG",
    val kommentar: String? = null,
    val hindreAutomatiskSvarbrev: Boolean? = null,
    val saksbehandlerIdentForTildeling: String? = null,
)

data class Journalposter(
    val type: String,
    val journalpostId: String,
)

private data class Fagsak(
    val fagsakId: String,
    val fagsystem: String,
)

private data class PersonIdent(
    val id: PersonIdentId,
)

private data class ProsessFullmektig(
    val id: PersonIdentId?,
    val navn: String?,
    val adresse: Adresse?,
)

private data class PersonIdentId(
    val type: String = "PERSON",
    val verdi: String,
)

private data class JournalpostIder(
    val journalpostIder: List<String>,
)

private data class Adresse(
    val addresselinje1: String?,
    val addresselinje2: String?,
    val addresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val land: String = "NO",
)

fun httpClient(
    prometheusRegistry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
    engine: HttpClientEngine = CIO.create { },
) = HttpClient(engine) {
    expectSuccess = true

    install(ContentNegotiation) {
        jackson {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    install(PrometheusMetricsPlugin) {
        this.baseName = "dp_saksbehandling_klage_http_klient"
        this.registry = prometheusRegistry
    }
}
