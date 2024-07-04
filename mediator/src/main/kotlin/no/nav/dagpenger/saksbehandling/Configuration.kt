package no.nav.dagpenger.saksbehandling

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.dagpenger.saksbehandling.streams.kafka.KafkaConfiguration
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
                "GRUPPE_BESLUTTER" to "BeslutterADGruppe",
                "JOURNALPOSTID_API_URL" to "http://dp-oppslag-journalpost-id/v1/journalpost",
                "JOURNALPOSTID_API_SCOOPE" to "api://dev-gcp.teamdagpenger.dp-oppslag-journalpost-id/.default",
                "SKJERMING_API_URL" to "http://skjermede-personer-pip.nom/skjermet",
                "SKJERMING_API_SCOPE" to "api://dev-gcp.nom.skjermede-personer-pip/.default",
                "SKJERMING_TOPIC" to "nom.skjermede-personer-status-v1",
                "PDL_API_SCOPE" to "api://dev-fss.pdl.pdl-api/.default",
                "PDL_API_URL" to "https://pdl-api.dev-fss-pub.nais.io:",
            ),
        )
    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val config: Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }

    val journalpostIdApiUrl: String = properties[Key("JOURNALPOSTID_API_URL", stringType)]
    val journalpostApiScope: String = properties[Key("JOURNALPOSTID_API_SCOOPE", stringType)]

    val journalpostTokenProvider = {
        azureAdClient.clientCredentials(journalpostApiScope).accessToken
    }

    val skjermingApiUrl: String = properties[Key("SKJERMING_API_URL", stringType)]
    val skjermingApiScope: String = properties[Key("SKJERMING_API_SCOPE", stringType)]
    val skjermingPersonStatusTopic: String = properties[Key("SKJERMING_TOPIC", stringType)]
    val skjermingTokenProvider = {
        azureAdClient.clientCredentials(skjermingApiScope).accessToken
    }

    val pdlUrl: String = properties[Key("PDL_API_URL", stringType)]
    val pdlApiScope: String = properties[Key("PDL_API_SCOPE", stringType)]
    val pdlTokenProvider = {
        azureAdClient.clientCredentials(pdlApiScope).accessToken
    }

    val saksbehandlerADGruppe by lazy { properties[Key("GRUPPE_SAKSBEHANDLER", stringType)] }
    val beslutterADGruppe by lazy { properties[Key("GRUPPE_BESLUTTER", stringType)] }

    val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }

    val skjermingsConsumerId = "dp-saksbehandling-skjerming-consumer-v0.0.4"
    val kafkaStreamProperties by lazy {
        KafkaConfiguration.kafkaStreamsConfiguration(
            applicationId = skjermingsConsumerId,
        )
    }
}
