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
import kotlinx.html.br
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.li
import kotlinx.html.meta
import kotlinx.html.section
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.title
import kotlinx.html.tr
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
                                    text(
                                        """
                                        ${behandling.navn} - Tilstand: '${behandling.tilstand.name}' (opprettet fra hendelse med id '${behandling.hendelseId}')
                                        """.trimIndent(),
                                    )
                                    br { }
                                    text("Aktivitetslogg")
                                    br { }
                                    table {
                                        tr {
                                            th {
                                                text("Type")
                                            }
                                            th {
                                                text("melding")
                                            }
                                        }
                                        for (aktivitet in behandling.aktivitetslogg)
                                            tr {
                                                td {
                                                    text(aktivitet.type)
                                                }
                                                td {
                                                    text(aktivitet.melding)
                                                }
                                            }
                                    }
                                    br { }

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

    override fun preVisit(
        behandling: no.nav.dagpenger.behandling.Behandling<*>,
        behandlingsId: UUID,
        hendelseId: UUID,
    ) {
        this.behandlingBygger = Behandling.Builder().navn(
            "Ny rettighetsbehandling",
        ).uuid(behandlingsId).hendelseId(hendelseId)
    }

    override fun visitSevere(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Severe,
        melding: String,
        tidsstempel: String,
    ) {
        this.behandlingBygger?.aktivitet(Behandling.Aktivitet(aktivitet.javaClass.simpleName, aktivitet.toString()))
    }

    override fun visitError(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Error,
        melding: String,
        tidsstempel: String,
    ) {
        this.behandlingBygger?.aktivitet(Behandling.Aktivitet(aktivitet.javaClass.simpleName, aktivitet.toString()))
    }

    override fun visitWarn(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Warn,
        melding: String,
        tidsstempel: String,
    ) {
        this.behandlingBygger?.aktivitet(Behandling.Aktivitet(aktivitet.javaClass.simpleName, aktivitet.toString()))
    }

    override fun visitBehov(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Behov,
        type: Aktivitetslogg.Aktivitet.Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any>,
        tidsstempel: String,
    ) {
        this.behandlingBygger?.aktivitet(Behandling.Aktivitet(aktivitet.javaClass.simpleName, aktivitet.toString()))
    }

    override fun visitInfo(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Info,
        melding: String,
        tidsstempel: String,
    ) {
        this.behandlingBygger?.aktivitet(Behandling.Aktivitet(aktivitet.javaClass.simpleName, aktivitet.toString()))
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

    override fun postVisit(
        behandling: no.nav.dagpenger.behandling.Behandling<*>,
        behandlingsId: UUID,
        hendelseId: UUID,
    ) {
        this.behandlinger.add(behandlingBygger!!.build())
        this.behandlingBygger = null
    }

    data class Behandling constructor(
        val navn: String,
        val uuid: UUID,
        val hendelseId: UUID,
        val tilstand: Type,
        val vilkår: List<Vilkår>,
        val aktivitetslogg: List<Aktivitet>,
    ) {

        constructor(builder: Builder) : this(
            navn = requireNotNull(builder.navn),
            uuid = requireNotNull(builder.uuid),
            hendelseId = requireNotNull(builder.hendelseId),
            tilstand = requireNotNull(builder.tilstand),
            vilkår = builder.vilkår.toList(),
            aktivitetslogg = builder.aktivitetslogg.toList(),
        )

        data class Vilkår(val uuid: UUID, val navn: String, val tilstand: Vilkårsvurdering.Tilstand.Type)
        data class Aktivitet(val type: String, val melding: String)
        class Builder {
            var navn: String? = null
                private set
            var uuid: UUID? = null
                private set
            var hendelseId: UUID? = null
                private set
            var tilstand: Type? = null
                private set
            val aktivitetslogg = mutableListOf<Aktivitet>()

            val vilkår = mutableListOf<Vilkår>()
            fun navn(navn: String) = apply { this.navn = navn }
            fun uuid(uuid: UUID) = apply { this.uuid = uuid }
            fun hendelseId(hendelseId: UUID) = apply { this.hendelseId = hendelseId }
            fun tilstand(tilstand: Type) = apply { this.tilstand = tilstand }
            fun vilkår(vilkår: Vilkår) = apply { this.vilkår.add(vilkår) }
            fun aktivitet(aktivitet: Aktivitet) = apply { this.aktivitetslogg.add(aktivitet) }
            fun build() = Behandling(this)
        }
    }
}
