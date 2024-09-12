package no.nav.dagpenger.saksbehandling.jwt

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPayloadHolder
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.request.ApplicationRequest
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.Saksbehandler

internal fun ApplicationCall.saksbehandler(): Saksbehandler {
    return requireNotNull(this.authentication.principal<JWTPrincipal>()) {
        "Ikke autentisert"
    }.let {
        Saksbehandler(
            navIdent = it.navIdent,
            grupper = it.payload.claims["groups"]?.asList(String::class.java)?.toSet() ?: setOf(),
            token = this.request.jwt(),
        )
    }
}

internal val JWTPrincipal.navIdent get(): String = requireNavIdent(this)

private fun requireNavIdent(credential: JWTPayloadHolder): String =
    requireNotNull(credential.payload.claims["NAVident"]?.asString()) { "Token må inneholde 'NAVident' claim" }

internal fun ApplicationCall.navIdent(): String =
    requireNotNull(this.authentication.principal<JWTPrincipal>()) { "Ikke autentisert" }.navIdent

internal fun ApplicationRequest.jwt(): String =
    this.parseAuthorizationHeader().let { authHeader ->
        (authHeader as? HttpAuthHeader.Single)?.blob ?: throw IllegalArgumentException("JWT not found")
    }
