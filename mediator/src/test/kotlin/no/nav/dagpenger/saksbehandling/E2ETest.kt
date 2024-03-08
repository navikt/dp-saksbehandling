package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.api.mockAzure
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.oppgaveApi
import no.nav.dagpenger.saksbehandling.db.InMemoryRepository
import no.nav.dagpenger.saksbehandling.maskinell.BehandlingKlient
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

class E2ETest {
    private val testIdent = "12345612345"
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()

    private val testRapid = TestRapid()
    private val inMemoryRepository = InMemoryRepository()
    private val testToken by mockAzure {
        claims = mapOf("NAVident" to "123")
    }

    private val testTokenProvider: (String, String) -> String = { _, _ -> "token" }
    private val baseUrl = "http://baseUrl"
    val mockEngine =
        MockEngine { request ->
            respond("/behandlingResponse.json".fileAsText(), headers = headersOf("Content-Type", "application/json"))
        }
    private val behandlingKlient =
        BehandlingKlient(
            behandlingUrl = baseUrl,
            behandlingScope = "scope",
            tokenProvider = testTokenProvider,
            engine = mockEngine,
        )
    private val mediator = Mediator(inMemoryRepository, inMemoryRepository, behandlingKlient)

    init {
        BehandlingOpprettetMottak(testRapid, mediator)
    }

    @AfterEach
    fun tearDown() {
        inMemoryRepository.slettAlt()
    }

    @Test
    fun `Skal opprette steg og opplysninger fra maskinell behandling når oppgave hentes`() {
        testRapid.sendTestMessage(søknadsbehandlingOpprettetMelding)
        val person = inMemoryRepository.hentBehandlingFra(testIdent)
        requireNotNull(person)
        person.ident shouldBe testIdent
        person.behandlinger.size shouldBe 1
        person.behandlinger.get(behandlingId)?.oppgave shouldNotBe null
        val oppgaveId = inMemoryRepository.hentAlleOppgaver().first().oppgaveId
        oppgaveId shouldNotBe null
        val oppgave = inMemoryRepository.hentBehandlingFra(oppgaveId)
        oppgave shouldNotBe null

        withOppgaveApi {
            client.get("/oppgave/$oppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val actualOppgave =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        OppgaveDTO::class.java,
                    )
                actualOppgave.steg.size shouldBe 2
                actualOppgave.steg[0].stegNavn shouldBe "Minsteinntekt"
                actualOppgave.steg[0].opplysninger.size shouldNotBe 0
                actualOppgave.steg[1].stegNavn shouldBe "Alder"
                actualOppgave.steg[1].opplysninger.size shouldNotBe 0
            }
        }
    }

    private fun withOppgaveApi(test: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            application { oppgaveApi(mediator) }
            test()
        }
    }

    private fun HttpRequestBuilder.autentisert() {
        header(HttpHeaders.Authorization, "Bearer $testToken")
    }

    // language=json
    private val søknadsbehandlingOpprettetMelding =
        """
        {
            "@event_name": "behandling_opprettet",
            "@opprettet": "2024-01-30T10:43:32.988331190",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "søknadId": "$søknadId",
            "behandlingId": "$behandlingId",
            "ident": "$testIdent"
        }
        """

    private fun String.fileAsText(): String {
        return object {}.javaClass.getResource(this)?.readText()
            ?: throw FileNotFoundException()
    }
}
