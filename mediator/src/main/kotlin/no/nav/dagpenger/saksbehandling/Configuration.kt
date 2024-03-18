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
                "GRUPPE_SAKSBEHANDLER" to "SaksbehandlerADGruppe",
                "DP_BEHANDLING_API_URL" to "http://dp-behandling",
                "SKJERMING_API_SCOPE" to "api://dev-gcp.nom.skjermede-personer-pip/.default",
                "SKJERMING_API_URL" to "http://skjermede-personer-pip.nom/skjermet",
            ),
        )
    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val config: Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }

    val skjermingApiUrl: String = properties[Key("SKJERMING_API_URL", stringType)]
    val skjermingApiScope: String = properties[Key("SKJERMING_API_URL", stringType)]

    val behandlingApiUrl: String = properties[Key("DP_BEHANDLING_API_URL", stringType)]
    val behandlingApiScope by lazy { properties[Key("DP_BEHANDLING_API_SCOPE", stringType)] }

    val saksbehandlerADGruppe by lazy { properties[Key("GRUPPE_SAKSBEHANDLER", stringType)] }

    val azureAdClient by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }
    val tilOboToken = { token: String, scope: String ->
        azureAdClient.onBehalfOf(token, scope).accessToken
    }

    val skjermingTokenProvider = { azureAdClient.clientCredentials(skjermingApiScope) }
}
