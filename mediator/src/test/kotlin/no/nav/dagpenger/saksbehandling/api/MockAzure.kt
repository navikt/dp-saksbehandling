package no.nav.dagpenger.saksbehandling.api

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.security.mock.oauth2.MockOAuth2Server

class MockAzure(
    private val config: MockConfig,
) {
    companion object {
        private const val AZURE_APP_CLIENT_ID = "test_client_id"
        private const val AZURE_OPENID_CONFIG_ISSUER = "test_issuer"
        private val mockOAuth2Server: MockOAuth2Server by lazy {
            MockOAuth2Server().also { server ->
                server.start()
                System.setProperty("AZURE_APP_CLIENT_ID", AZURE_APP_CLIENT_ID)
                System.setProperty("AZURE_OPENID_CONFIG_ISSUER", "${server.issuerUrl(AZURE_OPENID_CONFIG_ISSUER)}")
                System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", "${server.jwksUrl(AZURE_OPENID_CONFIG_ISSUER)}")
            }
        }

        fun lagTokenMedClaims(claims: Map<String, Any>): String =
            mockOAuth2Server
                .issueToken(
                    audience = AZURE_APP_CLIENT_ID,
                    issuerId = AZURE_OPENID_CONFIG_ISSUER,
                    claims = claims,
                ).serialize()

        fun HttpRequestBuilder.autentisert(token: String = gyldigSaksbehandlerToken()) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        fun gyldigSaksbehandlerToken(
            adGrupper: List<String> = emptyList(),
            navIdent: String = TestHelper.saksbehandler.navIdent,
        ): String =
            MockAzure.lagTokenMedClaims(
                mapOf(
                    "groups" to listOf("SaksbehandlerADGruppe") + adGrupper,
                    "NAVident" to navIdent,
                ),
            )

        fun gyldigMaskinToken(): String = lagTokenMedClaims(mapOf("idtyp" to "app"))
    }

    init {
        System.setProperty("AZURE_APP_CLIENT_ID", AZURE_APP_CLIENT_ID)
        System.setProperty("AZURE_OPENID_CONFIG_ISSUER", "${mockOAuth2Server.issuerUrl(AZURE_OPENID_CONFIG_ISSUER)}")
        System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", "${mockOAuth2Server.jwksUrl(AZURE_OPENID_CONFIG_ISSUER)}")
    }

    fun lagTokenMedClaims(claims: Map<String, Any>): String = MockAzure.lagTokenMedClaims(claims)
}

fun mockAzure(verifierConfig: MockConfig.() -> Unit = {}): MockAzure {
    val config = MockConfig().apply { verifierConfig() }
    return MockAzure(config)
}

data class MockConfig(
    var claims: Map<String, Any> = emptyMap(),
)
