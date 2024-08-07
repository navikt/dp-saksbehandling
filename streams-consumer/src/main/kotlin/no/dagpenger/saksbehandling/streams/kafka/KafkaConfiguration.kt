package no.dagpenger.saksbehandling.streams.kafka

import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.state.BuiltInDslStoreSuppliers.InMemoryDslStoreSuppliers

object KafkaConfiguration {
    fun kafkaStreamsConfiguration(applicationId: String): Map<String, String> {
        val configurations = systemProperties() overriding EnvironmentVariables
        return mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
            StreamsConfig.APPLICATION_ID_CONFIG to applicationId,
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to configurations[Key("KAFKA_BROKERS", stringType)],
            StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_CONFIG to InMemoryDslStoreSuppliers::class.java.name,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to configurations[Key("KAFKA_KEYSTORE_PATH", stringType)],
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to configurations[Key("KAFKA_CREDSTORE_PASSWORD", stringType)],
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to configurations[Key("KAFKA_TRUSTSTORE_PATH", stringType)],
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to configurations[Key("KAFKA_CREDSTORE_PASSWORD", stringType)],
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "jks",
        )
    }
}
