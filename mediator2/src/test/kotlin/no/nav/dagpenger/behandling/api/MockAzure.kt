package no.nav.dagpenger.behandling.api

import no.nav.security.mock.oauth2.MockOAuth2Server
import kotlin.reflect.KProperty

class MockAzure(private val config: MockConfig) {
    private companion object {
        private const val AZURE_APP_CLIENT_ID = "test_client_id"
        private const val AZURE_OPENID_CONFIG_ISSUER = "test_issuer"
        private val mockOAuth2Server: MockOAuth2Server by lazy {
            MockOAuth2Server().also { server ->
                server.start()
            }
        }
    }

    init {
        System.setProperty("AZURE_APP_CLIENT_ID", AZURE_APP_CLIENT_ID)
        System.setProperty("AZURE_OPENID_CONFIG_ISSUER", "${mockOAuth2Server.issuerUrl(AZURE_OPENID_CONFIG_ISSUER)}")
        System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", "${mockOAuth2Server.jwksUrl(AZURE_OPENID_CONFIG_ISSUER)}")
    }

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): Any =
        mockOAuth2Server.issueToken(
            audience = AZURE_APP_CLIENT_ID,
            issuerId = AZURE_OPENID_CONFIG_ISSUER,
            claims = config.claims,
        ).serialize()
}

fun mockAzure(init: MockConfig.() -> Unit): MockAzure {
    val config = MockConfig().apply { init() }
    return MockAzure(config)
}

data class MockConfig(var claims: Map<String, Any> = emptyMap())
