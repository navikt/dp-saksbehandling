package no.nav.dagpenger.behandling

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config

internal object Configuration {
    const val APP_NAME = "dp-behandling"

    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "RAPID_APP_NAME" to APP_NAME,
                "KAFKA_CONSUMER_GROUP_ID" to "dp-behandling-v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_EXTRA_TOPIC" to "teamdagpenger.journalforing.v1",
                "KAFKA_RESET_POLICY" to "latest",
            ),
        )
    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val config: Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }

    val dpIverksettUrl by lazy { properties[Key("DP_IVERKSETT_URL", stringType)] }
    val dpIverksettAudience by lazy { properties[Key("DP_IVERKSETT_AUDIENCE", stringType)] }

    val beslutterGruppe = properties[Key("GRUPPE_BESLUTTER", stringType)]
    val saksbehandlerGruppe = properties[Key("GRUPPE_SAKSBEHANDLER", stringType)]

    val tokenXClient by lazy {
        val tokenX = OAuth2Config.TokenX(properties)
        CachedOauth2Client(
            tokenEndpointUrl = tokenX.tokenEndpointUrl,
            authType = tokenX.privateKey(),
        )
    }
}
