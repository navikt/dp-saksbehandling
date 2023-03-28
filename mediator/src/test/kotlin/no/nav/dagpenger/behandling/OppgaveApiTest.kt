package no.nav.dagpenger.behandling

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OppgaveApiTest {
    private val jacksonObjectMapper = jacksonObjectMapper().also {
        it.registerModule(JavaTimeModule())
    }

    private fun withOppgaveApi(mediator: Mediator = Mediator(), test: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            application { oppgaveApi(mediator) }
            test()
        }
    }

    @Test
    fun `Skal kunne hente ut oppgaver`() {
        withOppgaveApi {
            client.get("/oppgaver").apply {
                val oppgaver: List<OppgaveDTO> = this.bodyAsText().let {
                    jacksonObjectMapper.readValue(it)
                }
                assertEquals(2, oppgaver.size)
            }
        }
    }

    @Test
    @Disabled
    fun `skal kunne svare på et steg`() {
        withOppgaveApi {
            val oppgaver: List<OppgaveDTO> = client.get("/oppgaver").let { response ->
                response.bodyAsText().let {
                    jacksonObjectMapper.readValue(it)
                }
            }

            val firstOppgave = oppgaver.first()
            val oppgaveId = firstOppgave.uuid
            val stegId = firstOppgave.steg.first().uuid

            client.put("/oppgaver/$oppgaveId/steg/$stegId") {
                contentType(ContentType.Application.Json)
                this.setBody(
                    //language=JSON
                    """
                    {
                      "svar": "2023-02-02",
                      "type": "LocalDate",
                      "begrunnelse": {
                        "kilde": "meg",
                        "tekst": "begrunnelse"
                      }
                    }
                    """.trimIndent(),
                )
            }.let { httpResponse ->
                assertEquals(200, httpResponse.status.value)
            }
        }
    }

    @Test
    fun `Skal kunne hente ut spesifikk oppgave`() {
        withOppgaveApi {
            val oppgaver: List<OppgaveDTO> = client.get("/oppgaver").let { response ->
                response.bodyAsText().let {
                    jacksonObjectMapper.readValue(it)
                }
            }

            client.get("/oppgaver/${oppgaver.first().uuid}").apply {
                assertEquals(200, this.status.value)
                assertTrue(this.contentType().toString().contains(ContentType.Application.Json.contentType))
            }
        }
    }
}
