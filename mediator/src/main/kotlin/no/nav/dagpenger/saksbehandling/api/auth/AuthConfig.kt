package no.nav.dagpenger.saksbehandling.api.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import no.nav.dagpenger.saksbehandling.Configuration

private val logger = KotlinLogging.logger {}

fun Application.authConfig() {
    install(Authentication) {
        jwt("azureAd") { jwtClaims ->
            jwtClaims.måInneholde(autorisertADGruppe = Configuration.saksbehandlerADGruppe)
            JWTPrincipal(jwtClaims.payload)
        }

        jwt("azureAd-maskin") { jwtClaims ->
            jwtClaims.måVæreApp()
            JWTPrincipal(jwtClaims.payload)
        }
    }
}

fun AuthenticationConfig.jwt(name: String) {
    jwt(name) {
        verifier(AzureAd)
        validate { jwtClaims ->
            jwtClaims.måInneholde(autorisertADGruppe = Configuration.saksbehandlerADGruppe)
            JWTPrincipal(jwtClaims.payload)
        }
    }
}

fun AuthenticationConfig.jwt(
    name: String,
    validateFunc: suspend ApplicationCall.(JWTCredential) -> JWTPrincipal,
) {
    jwt(name) {
        verifier(AzureAd)
        validate(validateFunc)
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

private fun JWTCredential.måVæreApp() {
    val erApp = this.payload.claims["idtyp"]?.asString() == "app"
    if (!erApp) {
        val errorMessage = "Credential inneholder ikke idtyp med verdi app"
        logger.warn { errorMessage }
        throw IllegalAccessException(errorMessage)
    }
}
