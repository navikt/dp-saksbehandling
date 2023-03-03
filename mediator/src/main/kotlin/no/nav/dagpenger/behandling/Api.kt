package no.nav.dagpenger.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
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
import no.nav.dagpenger.behandling.HtmlBygger.Vilkår
import no.nav.dagpenger.behandling.db.PersonRepository
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import no.nav.dagpenger.behandling.visitor.PersonVisitor
import java.lang.reflect.ParameterizedType
import java.util.UUID

internal fun Application.api(
    personRepository: PersonRepository,
) {
    install(DefaultHeaders)

    routing {
        route("behandlinger") {
            get("{ident}") {
                val ident = call.parameters["ident"] ?: throw MissingRequestParameterException("ident")
                val person = personRepository.hentPerson(ident) ?: throw NotFoundException("Kan ikke finne person")
                val bygger = HtmlBygger(person)
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
                            for (behandling in bygger.behandlinger) {
                                li { +behandling.key.navn }
                                ul {
                                    for (vilkår in behandling.value)
                                        li {
                                            text(
                                                """
                                                ${
                                                    vilkår.value.navn
                                                }  ${erOppfylt(vilkår.value.tilstand)}
                                                """.trimIndent(),
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

private class HtmlBygger(person: Person) : PersonVisitor {

    val behandlinger = mutableMapOf<Behandling, MutableMap<UUID, Vilkår>>()

    var behandlingsId: Behandling? = null

    init {
        person.accept(this)
    }

    override fun preVisit(behandlingsId: UUID, hendelseId: UUID) {
        this.behandlingsId = Behandling("Ny rettighetsbehandling", uuid = behandlingsId)
    }

    override fun <Vilkår : Vilkårsvurdering<Vilkår>> visitVilkårsvurdering(
        vilkårsvurderingId: UUID,
        tilstand: Vilkårsvurdering.Tilstand<Vilkår>,
    ) {
        this.behandlingsId?.let {
            val vilkår = behandlinger.getOrPut(it) { mutableMapOf() }
            vilkår[vilkårsvurderingId] = Vilkår(
                navn = (tilstand.javaClass.genericSuperclass as ParameterizedType)
                    .actualTypeArguments[0].typeName.substringAfterLast("."),
                tilstand = tilstand.tilstandType,
            )
        }
    }

    override fun postVisit(behandlingsId: UUID, hendelseId: UUID) {
        this.behandlingsId = null
    }

    data class Behandling(val navn: String, val uuid: UUID)
    data class Vilkår(val navn: String, val tilstand: Vilkårsvurdering.Tilstand.Type)
}

private fun erOppfylt(tilstand: Vilkårsvurdering.Tilstand.Type): String {
    return when (tilstand) {
        Vilkårsvurdering.Tilstand.Type.Oppfylt -> "✅ - Oppfylt "
        Vilkårsvurdering.Tilstand.Type.IkkeOppfylt -> "❌ - Ikke oppfylt"
        Vilkårsvurdering.Tilstand.Type.IkkeVurdert -> "❓ - Ikke vurdert "
        Vilkårsvurdering.Tilstand.Type.AvventerVurdering -> "⚒️ - Under behandling"
    }
}
