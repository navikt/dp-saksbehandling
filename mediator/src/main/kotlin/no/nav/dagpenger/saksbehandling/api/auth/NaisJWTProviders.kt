package no.nav.dagpenger.saksbehandling.api.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import java.net.URL

fun JWTAuthenticationProvider.Config.verifier(type: NaisJWTProviders) {
    type.createVerifier(this)
}

abstract class NaisJWTProviders private constructor(
    private val jwksUri: URL,
    private val issuer: String,
    private val clientId: String,
) {
    constructor(jwksUri: String, issuer: String, clientId: String) : this(
        jwksUri = URL(getEnvOrSystem(jwksUri)),
        issuer = getEnvOrSystem(issuer),
        clientId = getEnvOrSystem(clientId),
    )

    fun createVerifier(config: JWTAuthenticationProvider.Config) {
        config.verifier(
            JwkProviderBuilder(jwksUri).build(),
            issuer,
        ) { this.withAudience(clientId) }
    }
}

private fun getEnvOrSystem(name: String) = System.getenv(name) ?: System.getProperty(name)

object AzureAd : NaisJWTProviders("AZURE_OPENID_CONFIG_JWKS_URI", "AZURE_OPENID_CONFIG_ISSUER", "AZURE_APP_CLIENT_ID")

object TokenX : NaisJWTProviders("TOKEN_X_JWKS_URI", "TOKEN_X_ISSUER", "TOKEN_X_CLIENT_ID")
