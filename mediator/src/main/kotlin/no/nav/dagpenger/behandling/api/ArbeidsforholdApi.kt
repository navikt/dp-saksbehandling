package no.nav.dagpenger.behandling.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.dagpenger.behandling.api.auth.AzureAd
import no.nav.dagpenger.behandling.api.auth.verifier
import no.nav.dagpenger.behandling.arbeidsforhold.AaregClient

internal fun Application.arbeidsforholdApi(aaregClient: AaregClient) {
    install(CallLogging) {
        disableDefaultColors()
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            enable(SerializationFeature.INDENT_OUTPUT)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    install(Authentication) {
        jwt("azureAd") {
            verifier(AzureAd)
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }

    routing {
        authenticate("azureAd") {
            post("arbeidsforhold") {
                val ident = call.receive<Ident>()
                val arbeidsforhold = aaregClient.hentArbeidsforhold(ident.ident, call.request.jwt())
                call.respond(status = HttpStatusCode.OK, arbeidsforhold)
            }
        }
    }
}

internal data class Ident(val ident: String)
