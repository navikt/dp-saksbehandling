package no.nav.dagpenger.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.li
import kotlinx.html.meta
import kotlinx.html.title
import kotlinx.html.ul
import no.nav.dagpenger.behandling.db.PersonRepository
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering

internal fun Application.api(
    personRepository: PersonRepository,
) {

    install(DefaultHeaders)

    routing {
        route("behandlinger") {
            get("{ident}") {
                val ident = call.parameters["ident"] ?: throw MissingRequestParameterException("ident")
                val person = personRepository.hentPerson(ident) ?: throw NotFoundException("Kan ikke finne person")

                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title {
                            +"behandlinger"
                        }
                        meta {
                            charset = "utf-8"
                        }
                    }
                    body {
                        h1 {
                            +"Behandlinger for ${person.ident()}"
                        }
                        ul {
                            for (behandling in person.behandlinger()) {
                                li { +behandling.javaClass.simpleName }
                                ul {
                                    for (vilkår in behandling.vilkårsvurderinger)
                                        li {
                                            text(
                                                """
                                                ${
                                                    vilkår.javaClass.simpleName.replace(
                                                        "_",
                                                        " "
                                                    )
                                                }  ${erOppfylt(vilkår.tilstand)} 
                                                """.trimIndent()
                                            )
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun erOppfylt(tilstand: Vilkårsvurdering.Tilstand): String {
    return when(tilstand.tilstandType) {
        Vilkårsvurdering.Tilstand.Type.Oppfylt -> "✅ - Oppfylt "
        Vilkårsvurdering.Tilstand.Type.IkkeOppfylt -> "❌ - Ikke oppfylt"
        Vilkårsvurdering.Tilstand.Type.IkkeVurdert -> "❓ - Ikke vurdert "
        Vilkårsvurdering.Tilstand.Type.AvventerVurdering -> "⚒️ - Under behandling"
    }
}
