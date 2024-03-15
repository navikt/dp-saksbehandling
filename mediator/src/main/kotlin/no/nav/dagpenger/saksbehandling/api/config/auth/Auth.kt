package no.nav.dagpenger.saksbehandling.api.config.auth

import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Configuration

private val logger = KotlinLogging.logger {}
fun AuthenticationConfig.jwt(name: String) {
    jwt(name) {
        verifier(AzureAd)
        validate { jwtClaims ->
            jwtClaims.måInneholde(autorisertADGruppe = Configuration.saksbehandlerADGruppe)
            JWTPrincipal(jwtClaims.payload)
        }
    }
}

private fun JWTCredential.måInneholde(autorisertADGruppe: String) {
    val groups = this.payload.claims["groups"]?.asList(String::class.java)
    if (groups == null) {
        val errorMessage = "Credential inneholder ikke groups claim"
        logger.warn { errorMessage }
        throw IllegalAccessException(errorMessage)
    }

    if (!groups.contains(autorisertADGruppe)) {
        val errorMessage = "Credential inneholder ikke riktig gruppe. Forventet $autorisertADGruppe men var $groups"
        logger.warn { errorMessage }
        throw IllegalAccessException(errorMessage)
    }
}
