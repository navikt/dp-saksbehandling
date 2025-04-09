package no.nav.dagpenger.saksbehandling.saksbehandler

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.benmanes.caffeine.cache.Caffeine
import dev.hsbrysk.caffeine.CoroutineCache
import dev.hsbrysk.caffeine.buildCoroutine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.jackson.jackson
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Histogram
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger { }

interface SaksbehandlerOppslag {
    suspend fun hentSaksbehandler(navIdent: String): BehandlerDTO
}

internal class CachedSaksbehandlerOppslag(
    private val saksbehandlerOppslag: SaksbehandlerOppslag,
    registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
) : SaksbehandlerOppslag {
    private val cache: CoroutineCache<String, BehandlerDTO> =
        Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .buildCoroutine()

    private val counter = registry.lagCounter()

    override suspend fun hentSaksbehandler(navIdent: String): BehandlerDTO {
        var treff = true
        return cache.get(navIdent) {
            treff = false
            saksbehandlerOppslag.hentSaksbehandler(navIdent)
        }.also {
            when (treff) {
                true -> counter.hit()
                false -> counter.miss()
            }
        } ?: throw RuntimeException("Kunne ikke hente saksbehandler")
    }

    private fun Counter.hit() = this.labelValues("hit").inc()

    private fun Counter.miss() = this.labelValues("miss").inc()

    private fun PrometheusRegistry.lagCounter(): Counter =
        Counter.builder()
            .name("dp_saksbehandling_saksbehandler_oppslag_cache")
            .labelNames("treff")
            .help("Cache treff på saksbehandler oppslag")
            .register(this)
}

internal class SaksbehandlerOppslagImpl(
    private val norgBaseUrl: String = Configuration.norg2BaseUrl,
    private val msGraphBaseUrl: String = Configuration.msGraphBaseUrl,
    private val tokenProvider: () -> String,
    prometheusRegistry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
    engine: HttpClientEngine = CIO.create { },
) : SaksbehandlerOppslag {
    private val httpClient =
        HttpClient(engine = engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
        }

    private val histogram =
        Histogram.builder()
            .name("dp_saksbehandling_saksbehandler_oppslag_duration")
            .help("Tid brukt på oppslag av saksbehandler")
            .register(prometheusRegistry)

    override suspend fun hentSaksbehandler(navIdent: String): BehandlerDTO {
        return coroutineScope {
            val timer = histogram.startTimer()
            val user =
                httpClient.get(urlString = "$msGraphBaseUrl/v1.0/users") {
                    parameter("\$count", true)
                    parameter("\$filter", "onPremisesSamAccountName eq '$navIdent'")
                    parameter("\$select", "streetAddress,givenName,surname")
                    header("Authorization", "Bearer ${tokenProvider.invoke()}")
                    header("ConsistencyLevel", "eventual")
                }.body<UserResponse>().value.single()

            val enhetsNr = user.streetAddress
            val enhet =
                async(Dispatchers.IO) {
                    httpClient.get(urlString = "$norgBaseUrl/api/v1/enhet/$enhetsNr") {
                    }.body<Enhet>()
                }

            val kontaktInformasjon =
                async(Dispatchers.IO) {
                    httpClient.get(urlString = "$norgBaseUrl/api/v1/enhet/$enhetsNr/kontaktinformasjon") {
                    }.body<KontaktInformasjon>()
                }

            BehandlerDTO(
                ident = navIdent,
                fornavn = user.givenName,
                etternavn = user.surname,
                enhet =
                    BehandlerDTOEnhetDTO(
                        navn = enhet.await().navn,
                        enhetNr = enhetsNr,
                        postadresse = kontaktInformasjon.await().formatertPostAdresse(),
                    ),
            ).also {
                logger.info { "Hentet saksbehandler $it på ${timer.observeDuration()} nanosekunder" }
            }
        }
    }

    private data class Enhet(
        val navn: String,
    )

    private data class KontaktInformasjon(
        val postadresse: PostAdresse?,
    ) {
        fun formatertPostAdresse(): String {
            return postadresse?.formatertPostAdresse() ?: ""
        }

        data class PostAdresse(
            val postnummer: String,
            val poststed: String,
            val type: String,
            val postboksnummer: String,
            val postboksanlegg: String?,
        ) {
            fun postboksAnlegg(): String {
                return when (postboksanlegg) {
                    null -> ""
                    else -> "$postboksanlegg,"
                }
            }

            fun formatertPostAdresse(): String {
                return "Postboks $postboksnummer, ${postboksAnlegg()} $postnummer $poststed"
            }
        }
    }

    private data class UserResponse(
        val value: List<User>,
    )

    private data class User(
        val givenName: String,
        val surname: String,
        val streetAddress: String,
    )
}
