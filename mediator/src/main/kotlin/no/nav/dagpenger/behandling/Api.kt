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
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.li
import kotlinx.html.meta
import kotlinx.html.section
import kotlinx.html.title
import kotlinx.html.ul
import no.nav.dagpenger.behandling.Behandling.Tilstand.Type
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
                            +"Person ${person.ident()}"
                        }
                        section {
                            h2 {
                                +"Behandlinger"
                            }
                            ul {
                                for (behandling in bygger.behandlinger) {
                                    li {
                                        text(
                                            """
                                        ${behandling.navn} - Tilstand: '${behandling.tilstand.name}' (opprettet fra hendelse med id '${behandling.hendelseId}')
                                            """.trimIndent(),
                                        )
                                    }

                                    ul {
                                        for (vilkår in behandling.vilkår)
                                            li {
                                                text(
                                                    """
                                                ${
                                                        vilkår.navn
                                                    }  ${erOppfylt(vilkår.tilstand)}
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
}

private fun erOppfylt(tilstand: Vilkårsvurdering.Tilstand.Type): String {
    return when (tilstand) {
        Vilkårsvurdering.Tilstand.Type.Oppfylt -> "✅ - Oppfylt "
        Vilkårsvurdering.Tilstand.Type.IkkeOppfylt -> "❌ - Ikke oppfylt"
        Vilkårsvurdering.Tilstand.Type.IkkeVurdert -> "❓ - Ikke vurdert "
        Vilkårsvurdering.Tilstand.Type.AvventerVurdering -> "⚒️ - Under behandling"
    }
}

private class HtmlBygger(person: Person) : PersonVisitor {

    val behandlinger: MutableList<Behandling> = mutableListOf()

    private var behandlingBygger: Behandling.Builder? = null

    init {
        person.accept(this)
    }

    override fun preVisit(behandlingsId: UUID, hendelseId: UUID) {
        this.behandlingBygger = Behandling.Builder().navn(
            "Ny rettighetsbehandling",
        ).uuid(behandlingsId).hendelseId(hendelseId)
    }

    override fun visitTilstand(tilstand: Type) {
        this.behandlingBygger?.tilstand(tilstand)
    }

    override fun <Vilkår : Vilkårsvurdering<Vilkår>> visitVilkårsvurdering(
        vilkårsvurderingId: UUID,
        tilstand: Vilkårsvurdering.Tilstand<Vilkår>,
    ) {
        this.behandlingBygger?.vilkår(
            Behandling.Vilkår(
                uuid = vilkårsvurderingId,
                navn = (tilstand.javaClass.genericSuperclass as ParameterizedType)
                    .actualTypeArguments[0].typeName.substringAfterLast("."),
                tilstand = tilstand.tilstandType,
            ),
        )
    }

    override fun postVisit(behandlingsId: UUID, hendelseId: UUID) {
        this.behandlinger.add(behandlingBygger!!.build())
        this.behandlingBygger = null
    }

    data class Behandling constructor(
        val navn: String,
        val uuid: UUID,
        val hendelseId: UUID,
        val tilstand: Type,
        val vilkår: List<Vilkår>,
    ) {

        constructor(builder: Builder) : this(
            navn = requireNotNull(builder.navn),
            uuid = requireNotNull(builder.uuid),
            hendelseId = requireNotNull(builder.hendelseId),
            tilstand = requireNotNull(builder.tilstand),
            vilkår = builder.vilkår.toList(),
        )
        data class Vilkår(val uuid: UUID, val navn: String, val tilstand: Vilkårsvurdering.Tilstand.Type)
        class Builder {
            var navn: String? = null
                private set
            var uuid: UUID? = null
                private set
            var hendelseId: UUID? = null
                private set
            var tilstand: Type? = null
                private set

            val vilkår = mutableListOf<Vilkår>()
            fun navn(navn: String) = apply { this.navn = navn }
            fun uuid(uuid: UUID) = apply { this.uuid = uuid }
            fun hendelseId(hendelseId: UUID) = apply { this.hendelseId = hendelseId }
            fun tilstand(tilstand: Type) = apply { this.tilstand = tilstand }
            fun vilkår(vilkår: Vilkår) = apply { this.vilkår.add(vilkår) }
            fun build() = Behandling(this)
        }
    }
}
