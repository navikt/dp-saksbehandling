package no.nav.dagpenger.behandling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
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
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class OppgaveApiTest {

    private fun withOppgaveApi(
        mediator: Mediator = Mediator(mockPersistence),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application { oppgaveApi(mediator) }
            test()
        }
    }

    @Test
    fun `Skal kunne hente ut oppgaver`() {
        withOppgaveApi {
            client.get("/oppgaver").let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                response.bodyAsText() shouldEqualSpecifiedJson """[$testBehandlingJson]"""
            }
        }
    }

    private fun String.findStegUUID(id: String): String {
        jacksonObjectMapper().readTree(this).let { root ->
            return root["steg"].first { it["id"].asText() == id }["uuid"].asText()
        }
    }

    @Test
    fun `skal kunne svare på et steg`() {
        withOppgaveApi {
            val oppgaverJson: String = client.get("/oppgaver/${mockPersistence.behandlingId}").bodyAsText()

            val stegId = oppgaverJson.findStegUUID("vilkår1")

            client.put("/oppgaver/${mockPersistence.behandlingId}/steg/$stegId") {
                contentType(ContentType.Application.Json)
                this.setBody(
                    //language=JSON
                    """
                {
                    "svar": "false",
                    "type": "Boolean",
                    "begrunnelse": {
                    "kilde": "meg",
                    "tekst": "begrunnelse"
                }
                }
                    """.trimIndent(),
                )
            }.status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `Skal kunne hente ut spesifikk oppgave`() {
        withOppgaveApi {
            client.get("/oppgaver/${mockPersistence.behandlingId}").let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                response.bodyAsText() shouldEqualSpecifiedJson testBehandlingJson
            }
        }
    }

    private val mockPersistence = object : BehandlingRepository {
        val testPerson = Person("123")
        var behandlingId: UUID
        val behandlinger = listOf(
            behandling(testPerson) {
                steg {
                    vilkår("vilkår1") {
                        avhengerAvFastsettelse<LocalDate>("vilkår 1 dato")
                    }
                }
                steg {
                    fastsettelse<Int>("fastsettelse1")
                }
            }.also { behandlingId = it.uuid },
        )

        override fun hentBehandlinger() = behandlinger
        override fun hentBehandling(behandlingUUID: UUID) = behandlinger.single { it.uuid == behandlingUUID }
    }

    @Language("JSON")
    val testBehandlingJson = """
       {
"uuid": "${mockPersistence.behandlingId}",
"person": "123",
"saksbehandler": null,
"hendelse": [],
"steg": [
  {
    "id": "vilkår1",
    "type": "Vilkår",
    "svartype": "Boolean",
    "tilstand": "IkkeUtført",
    "svar": {
      "svar": "null",
      "type": "Boolean"
    }
  },
  {
    "id": "vilkår 1 dato",
    "type": "Fastsetting",
    "svartype": "LocalDate",
    "tilstand": "IkkeUtført",
    "svar": {
      "svar": "null",
      "type": "LocalDate",
      "begrunnelse": {
        "kilde": "",
        "tekst": ""
      }
    }
  },
  {
    "id": "fastsettelse1",
    "type": "Fastsetting",
    "svartype": "Int",
    "tilstand": "IkkeUtført",
    "svar": {
      "svar": "null",
      "type": "Int",
      "begrunnelse": {
        "kilde": "",
        "tekst": ""
      }
    }
  }
]
}
    """.trimIndent()
}
