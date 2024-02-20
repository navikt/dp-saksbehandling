package no.nav.dagpenger.saksbehandling

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config

internal object Configuration {
    const val APP_NAME = "dp-saksbehandling"

    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "RAPID_APP_NAME" to APP_NAME,
                "KAFKA_CONSUMER_GROUP_ID" to "dp-saksbehandling-v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_RESET_POLICY" to "latest",
                "GRUPPE_BESLUTTER" to "123",
                "GRUPPE_SAKSBEHANDLER" to "456",
                "DP_BEHANDLING_API_URL" to "http://dp-behandling",
            ),
        )
    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val config: Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }

    val behandlingUrl: String = properties[Key("DP_BEHANDLING_API_URL", stringType)]
    val dpBehandlingScope by lazy { properties[Key("DP_BEHANDLING_API_SCOPE", stringType)] }

    val azureAdClient by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }
}
