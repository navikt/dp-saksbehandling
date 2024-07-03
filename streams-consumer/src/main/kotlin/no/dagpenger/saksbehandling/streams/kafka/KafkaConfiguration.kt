package no.dagpenger.saksbehandling.streams.kafka

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.state.BuiltInDslStoreSuppliers.InMemoryDslStoreSuppliers

object KafkaConfiguration {
    private val localProperties =
        ConfigurationMap(
            mapOf(
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.PLAINTEXT.name,
                SaslConfigs.SASL_MECHANISM to "PLAIN",
            ),
        )
    private val naisProperties =
        ConfigurationMap(
            mapOf(
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
                SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
                SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to "",
                SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to "",
                SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
                SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to "",
                SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to "",
                SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "jks",
            ),
        )

    fun kafkaStreamsConfiguration(applicationId: String): Map<String, String> {
        val configuration =
            when (System.getenv("NAIS_CLUSTER_NAME")) {
                null -> systemProperties() overriding EnvironmentVariables overriding naisProperties
                else -> localProperties
            }
        return configuration.list().reversed().fold(
            mapOf(
                StreamsConfig.APPLICATION_ID_CONFIG to applicationId,
                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to configuration[Key("KAFKA_BROKERS", stringType)],
                StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_CONFIG to InMemoryDslStoreSuppliers::class.java.name,
            ),
        ) { map, pair ->
            map + pair.second
        }
    }
}
