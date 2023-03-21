package no.nav.dagpenger.behandling

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals

class OppgaveApiTest {
    private val jacksonObjectMapper = jacksonObjectMapper().also {
        it.registerModule(JavaTimeModule())
    }

    @Test
    fun `Skal kunne hente ut oppgaver`() {
        testApplication {
            application {
                oppgaveApi()
            }
            client.get("/oppgaver").apply {
                val oppgaver: List<Oppgave> = this.bodyAsText().let {
                    jacksonObjectMapper.readValue(it, object : TypeReference<List<Oppgave>>() {})
                }
                assertEquals(2, oppgaver.size)
            }
        }
    }

    @Test
    fun `skal kunne svare på et steg`() {
        testApplication {
            application {
                oppgaveApi()
            }
            client.put("/oppgaver/oppgaveId/steg/stegId") {
                contentType(ContentType.Application.Json)
                this.setBody(
                    """ {"svar": "123", "begrunnelse": "begrunnelse"}
                    """.trimIndent(),
                )
            }.let { httpResponse ->

                assertEquals(200, httpResponse.status.value)
            }
        }
    }

    @Test
    fun `Skal kunne hente ut spesifikk oppgave`() {
        testApplication {
            application {
                oppgaveApi()
            }
            client.get("/oppgaver/123").apply {
                assertEquals(200, this.status.value)

                assertDoesNotThrow {
                    this.bodyAsText().let {
                        jacksonObjectMapper.readValue(it, Oppgave::class.java)
                    }
                }
            }
        }
    }
}
