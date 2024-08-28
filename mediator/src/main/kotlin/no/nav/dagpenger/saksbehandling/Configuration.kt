package no.nav.dagpenger.saksbehandling

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import no.nav.dagpenger.saksbehandling.streams.kafka.KafkaConfiguration

object Configuration {
    const val APP_NAME = "dp-saksbehandling"

    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "RAPID_APP_NAME" to APP_NAME,
                "KAFKA_CONSUMER_GROUP_ID" to "dp-saksbehandling-v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_RESET_POLICY" to "latest",
                "GRUPPE_EGNE_ANSATTE" to "EgneAnsatteADGruppe",
                "GRUPPE_SAKSBEHANDLER" to "SaksbehandlerADGruppe",
                "GRUPPE_BESLUTTER" to "BeslutterADGruppe",
                "GRUPPE_FORTROLIG" to "FortroligADGruppe",
                "GRUPPE_STRENGT_FORTROLIG_UTLAND" to "StrengtFortroligUtlandADGruppe",
                "GRUPPE_STRENGT_FORTROLIG" to "StrengtFortroligADGruppe",
                "JOURNALPOSTID_API_URL" to "http://dp-oppslag-journalpost-id/v1/journalpost",
                "JOURNALPOSTID_API_SCOOPE" to "api://dev-gcp.teamdagpenger.dp-oppslag-journalpost-id/.default",
                "SKJERMING_API_URL" to "http://skjermede-personer-pip.nom/skjermet",
                "SKJERMING_API_SCOPE" to "api://dev-gcp.nom.skjermede-personer-pip/.default",
                "SKJERMING_TOPIC" to "nom.skjermede-personer-status-v1",
                "LEESAH_TOPIC" to "pdl.leesah-v1",
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

    val journalpostTokenProvider = { clientCredentialsTokenProvider(journalpostApiScope) }

    val skjermingApiUrl: String = properties[Key("SKJERMING_API_URL", stringType)]
    val skjermingApiScope: String = properties[Key("SKJERMING_API_SCOPE", stringType)]
    val skjermingPersonStatusTopic: String = properties[Key("SKJERMING_TOPIC", stringType)]
    val leesahTopic: String = properties[Key("LEESAH_TOPIC", stringType)]
    val skjermingTokenProvider = { clientCredentialsTokenProvider(skjermingApiScope) }

    private val clientCredentialsTokenProvider = { scope: String ->
        azureAdClient.clientCredentials(scope).accessToken
            ?: throw RuntimeException("Failed to get access token")
    }

    val pdlUrl: String = properties[Key("PDL_API_URL", stringType)]
    val pdlApiScope: String = properties[Key("PDL_API_SCOPE", stringType)]
    val pdlTokenProvider = { clientCredentialsTokenProvider(pdlApiScope) }

    val saksbehandlerADGruppe by lazy { properties[Key("GRUPPE_SAKSBEHANDLER", stringType)] }
    val egneAnsatteADGruppe by lazy { properties[Key("GRUPPE_EGNE_ANSATTE", stringType)] }
    val strengtFortroligADGruppe by lazy { properties[Key("GRUPPE_STRENGT_FORTROLIG", stringType)] }
    val strengtFortroligUtlandADGruppe by lazy { properties[Key("GRUPPE_STRENGT_FORTROLIG_UTLAND", stringType)] }
    val fortroligADGruppe by lazy { properties[Key("GRUPPE_FORTROLIG", stringType)] }

    val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }

    val kafkaStreamsConsumerId = "dp-saksbehandling-streams-consumer-v1"
    val kafkaStreamProperties by lazy {
        KafkaConfiguration.kafkaStreamsConfiguration(consumerId = kafkaStreamsConsumerId)
    }
}
