package no.nav.dagpenger.behandling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import no.nav.dagpenger.behandling.persistence.BehandlingRepository
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BehandlingApiTest {

    private fun withBehandlingApi(
        mediator: Mediator = Mediator(mockPersistence),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application { behandlingApi(mediator) }
            test()
        }
    }

    @Test
    fun `Skal kunne hente ut alle behandlinger`() {
        withBehandlingApi {
            client.get("/behandlinger").let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"

                val behandlinger = jacksonObjectMapper().readTree(response.bodyAsText())
                behandlinger.size() shouldBe 3
            }
        }
    }

    @Test
    fun `skal kunne svare på et steg`() {
        withBehandlingApi {
            val behandlingerJson: String = client.get("/behandlinger/${mockPersistence.behandlingId1}").bodyAsText()

            val stegId = behandlingerJson.findStegUUID("vilkår1")

            client.put("/behandlinger/${mockPersistence.behandlingId1}/steg/$stegId") {
                contentType(ContentType.Application.Json)
                this.setBody(
                    //language=JSON
                    """{"type":"Boolean","svar":true,"begrunnelse":{"tekst":"Har itte","kilde":"Høggern"}}""".trimIndent(),
                )
            }.status shouldBe HttpStatusCode.OK
        }
    }

    private fun String.findStegUUID(id: String): String {
        jacksonObjectMapper().readTree(this).let { root ->
            return root["steg"].first { it["id"].asText() == id }["uuid"].asText()
        }
    }

    @Test
    fun `Skal kunne hente ut en behandling med en gitt id`() {
        withBehandlingApi {
            client.get("/behandlinger/${mockPersistence.behandlingId1}").also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"

                val behandling = jacksonObjectMapper().readTree(response.bodyAsText())
                behandling.isObject shouldBe true
                behandling["uuid"].asText() shouldBe mockPersistence.behandlingId1.toString()
            }
        }
    }

    @Test
    fun `Får 404 Not Found ved forsøk på å hente behandling som ikke finnes`() {
        withBehandlingApi {
            client.get("/behandlinger/${UUID.randomUUID()}").also { response ->
                response.status shouldBe HttpStatusCode.NotFound
            }
        }
    }

    @Test
    fun `Skal kunne hente ut alle behandlinger for en gitt person`() {
        withBehandlingApi {
            client.post("/behandlinger/sok") {
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"fnr": ${mockPersistence.testPerson1.ident}}""",
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"

                val behandlinger = jacksonObjectMapper().readTree(response.bodyAsText())
                behandlinger.size() shouldBe 2
            }
        }
    }

    @Test
    fun `Får 404 Not Found dersom det ikke finnes behandlinger for et gitt fnr`() {
        withBehandlingApi {
            client.post("/behandlinger/sok") {
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"fnr": "789"}""",
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.NotFound
            }
        }
    }

    private val mockPersistence = object : BehandlingRepository {
        val testPerson1 = Person("123")
        val testPerson2 = Person("456")
        var behandlingId1: UUID
        var behandlingId2: UUID
        var behandlingId3: UUID

        val behandlinger = listOf(
            behandling(testPerson1) {
                steg {
                    vilkår("vilkår1") {
                        avhengerAvFastsettelse<LocalDate>("vilkår 1 dato")
                    }
                }
                steg {
                    fastsettelse<Int>("fastsettelse1")
                }
            }.also { behandlingId1 = it.uuid },

            behandling(testPerson1) {
                steg {
                    vilkår("vilkår2")
                }
            }.also { behandlingId2 = it.uuid },

            behandling(testPerson2) {
                steg {
                    vilkår("vilkår3")
                }
            }.also { behandlingId3 = it.uuid },
        )

        override fun hentBehandlinger() = behandlinger

        override fun hentBehandling(behandlingUUID: UUID): Behandling {
            return behandlinger.firstOrNull { behandling ->
                behandling.uuid == behandlingUUID
            } ?: throw NoSuchElementException()
        }

        override fun hentBehandlingerFor(fnr: String): List<Behandling> {
            val behandlingerForFnr = behandlinger.filter { behandling ->
                behandling.person.ident == fnr
            }.takeIf {
                it.isNotEmpty()
            }

            return behandlingerForFnr ?: throw NoSuchElementException()
        }
    }
}
