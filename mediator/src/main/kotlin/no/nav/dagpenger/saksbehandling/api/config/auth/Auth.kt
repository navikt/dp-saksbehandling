package no.nav.dagpenger.saksbehandling.api.config.auth

import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import no.nav.dagpenger.saksbehandling.Configuration

fun AuthenticationConfig.jwt(name: String) {
    jwt(name) {
        verifier(AzureAd)
        validate { jwtClaims ->
            jwtClaims.måInneholde(autorisertADGruppe = Configuration.saksbehandlerADGruppe)
            JWTPrincipal(jwtClaims.payload)
        }
    }
}

private fun JWTCredential.måInneholde(autorisertADGruppe: String) =
    require(this.payload.claims["groups"]?.asList(String::class.java)?.contains(autorisertADGruppe) ?: false)
