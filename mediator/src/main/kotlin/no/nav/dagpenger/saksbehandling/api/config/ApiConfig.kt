package no.nav.dagpenger.saksbehandling.api.config

import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.dagpenger.saksbehandling.api.config.auth.AzureAd
import no.nav.dagpenger.saksbehandling.api.config.auth.verifier

fun Application.apiConfig() {
    install(CallLogging) {
        disableDefaultColors()
    }

    install(ContentNegotiation) {
        jackson {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
    }

    install(Authentication) {
        jwt("azureAd") {
            verifier(AzureAd)
            // configFor(autorisertADGruppe = saksbehandlerADGruppe)
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }
}

internal fun JWTAuthenticationProvider.Config.configFor(autorisertADGruppe: String) {
    validate { jwtClaims ->
        jwtClaims.måInneholde(ADGruppe = autorisertADGruppe)
        JWTPrincipal(jwtClaims.payload)
    }
}

private fun JWTCredential.måInneholde(ADGruppe: String) =
    require(this.payload.claims["groups"]?.asList(String::class.java)?.contains(ADGruppe) ?: false)
