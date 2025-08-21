package no.nav.dagpenger.saksbehandling.leaderelection

import com.fasterxml.jackson.databind.DeserializationFeature
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.jackson.jackson
import no.nav.dagpenger.saksbehandling.Configuration
import java.net.InetAddress

object LeaderElector {
    private val log = KotlinLogging.logger {}

    private val httpClient =
        HttpClient {
            install(ContentNegotiation) {
                jackson {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
            install(Logging) {
                level = LogLevel.BODY
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            log.info { message }
                        }
                    }
            }
        }

    private data class Leader(val name: String)

    suspend fun isLeader(): Result<Boolean> {
        return kotlin.runCatching {
            val url = Configuration.properties[Key("ELECTOR_GET_URL", stringType)]
            val hostName = InetAddress.getLocalHost().hostName
            httpClient.get(url).body<Leader>().let {
                log.info {
                    "leader is ${it.name}, I am $hostName"
                }
                it.name == hostName
            }
        }
    }
}
