package no.nav.dagpenger.saksbehandling.streams.kafka

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.state.BuiltInDslStoreSuppliers.InMemoryDslStoreSuppliers

object KafkaConfiguration {
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "KAFKA_SCHEMA_REGISTRY" to "mock://localhost:8081",
                "KAFKA_SCHEMA_REGISTRY_USER" to "username",
                "KAFKA_SCHEMA_REGISTRY_PASSWORD" to "password",
            ),
        )

    fun kafkaStreamsConfiguration(consumerId: String): Map<String, String> {
        val configurations = systemProperties() overriding EnvironmentVariables
        return mapOf(
            StreamsConfig.APPLICATION_ID_CONFIG to consumerId,
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to configurations[Key("KAFKA_BROKERS", stringType)],
            StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_CONFIG to InMemoryDslStoreSuppliers::class.java.name,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to configurations[Key("KAFKA_KEYSTORE_PATH", stringType)],
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to configurations[Key("KAFKA_CREDSTORE_PASSWORD", stringType)],
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to configurations[Key("KAFKA_TRUSTSTORE_PATH", stringType)],
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to configurations[Key("KAFKA_CREDSTORE_PASSWORD", stringType)],
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "jks",
        ) + kafkaSchemaRegistryConfiguration()
    }

    fun kafkaSchemaRegistryConfiguration(): Map<String, String> {
        val configurations = systemProperties() overriding EnvironmentVariables overriding defaultProperties
        return mapOf(
            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to
                configurations[Key("KAFKA_SCHEMA_REGISTRY", stringType)],
            AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
            SchemaRegistryClientConfig.USER_INFO_CONFIG to
                configurations[Key("KAFKA_SCHEMA_REGISTRY_USER", stringType)] +
                ":${configurations[Key("KAFKA_SCHEMA_REGISTRY_PASSWORD", stringType)]}",
        )
    }
}
