package no.nav.dagpenger.saksbehandling.api

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.MinsteInntektSteg
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Opplysning
import no.nav.dagpenger.saksbehandling.OpplysningStatus
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.StegTilstandDTO
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class OppgaveApiTest {
    val testIdent = "13083826694"
    val testBeskrivendeId = "steg-test"
    private val mockAzure = mockAzure()

    private val gyldigToken = mockAzure.lagTokenMedClaims(mapOf("groups" to listOf("SaksbehandlerADGruppe")))

    @Test
    fun `Skal avvise kall uten autoriserte AD grupper`() {
        withOppgaveApi {
            client.get("/oppgave") { autentisert(token = mockAzure.lagTokenMedClaims(mapOf("groups" to "UgyldigADGruppe"))) }
                .status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Skal kunne hente ut alle oppgaver`() {
        val mediatorMock = mockk<Mediator>().also {
            every { it.hentOppgaverKlarTilBehandling() } returns listOf(
                lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
                lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
            )
        }

        withOppgaveApi(mediatorMock) {
            client.get("/oppgave") { autentisert() }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgaver =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        object : TypeReference<List<OppgaveDTO>>() {},
                    )
                oppgaver.size shouldBe 2
            }
        }
    }

    @Test
    fun `Når saksbehandler henter en oppgave, oppdater den med steg og opplysninger`() {
        val mediatorMock = mockk<Mediator>()
        val oppgaveId = UUIDv7.ny()
        val oppgave = testOppgaveFerdigBehandlet(oppgaveId)

        coEvery { mediatorMock.oppdaterOppgaveMedSteg(any()) } returns oppgave

        withOppgaveApi(mediator = mediatorMock) {
            client.get("/oppgave/$oppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val actualOppgave =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        OppgaveDTO::class.java,
                    )
                actualOppgave.steg.size shouldBe 1
                actualOppgave.steg[0].beskrivendeId shouldBe MinsteInntektSteg.MINSTEINNTEKT_BESKRIVENDE_ID
                actualOppgave.steg[0].tilstand shouldBe StegTilstandDTO.OPPFYLT
            }
        }
    }

    @Test
    fun `Når saksbehandler bekrefter opplysninger i en oppgave skal behandlingen bekreftes og oppgavens tilstand bli ferdigbehandlet`() {
        val mediatorMock = mockk<Mediator>(relaxed = true)
        val oppgaveId = UUIDv7.ny()
        val ferdigbehandletOppgave = testOppgaveFerdigBehandlet(oppgaveId)

        coEvery { mediatorMock.bekreftOppgavensOpplysninger(any()) } returns Result.success(Unit)

        withOppgaveApi(mediator = mediatorMock) {
            client.put("/oppgave/$oppgaveId/avslag") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
        }
        ferdigbehandletOppgave.tilstand shouldBe Oppgave.Tilstand.Type.FERDIG_BEHANDLET
    }

    @Test
    @Disabled("Venter på endepunkt i dp-behandling")
    fun `Når saksbehandler avbryter en oppgave skal behandlingen avbrytes og oppgavens tilstand bli ferdigbehandlet`() {
        val mediatorMock = mockk<Mediator>()
        val oppgaveId = UUIDv7.ny()
        val oppgave = testOppgaveFerdigBehandlet(oppgaveId)

        coEvery { mediatorMock.avbrytBehandling(any()) } returns Result.success(Unit)
        withOppgaveApi {
            client.put("/oppgave/$oppgaveId/lukk") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
        }
        oppgave.tilstand shouldBe Oppgave.Tilstand.Type.FERDIG_BEHANDLET
    }

    @Test
    fun `Får 404 Not Found ved forsøk på å hente oppgave som ikke finnes`() {
        val ikkeEksisterendeOppgaveId = UUIDv7.ny()
        val mediator =
            mockk<Mediator>().also {
                coEvery { it.oppdaterOppgaveMedSteg(any()) } returns null
            }
        withOppgaveApi(mediator) {
            client.get("/oppgave/$ikkeEksisterendeOppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe "Fant ingen oppgave med UUID $ikkeEksisterendeOppgaveId"
            }
        }
    }

    @Test
    fun `Får feil ved ugyldig oppgaveId`() {
        val ugyldigId = "noeSomIkkeKanParsesTilUUID"
        withOppgaveApi {
            shouldThrow<IllegalArgumentException> {
                client.get("/oppgave/$ugyldigId") { autentisert() }
            }
        }
    }

    @Test
    fun `Skal kunne hente ut alle oppgaver for en gitt person`() {
        val mediatorMock = mockk<Mediator>().also {
            every { it.finnOppgaverFor(testIdent) } returns listOf(
                testOppgaveFerdigBehandlet(UUIDv7.ny()),
                testOppgaveFerdigBehandlet(UUIDv7.ny()),
            )
        }
        withOppgaveApi(mediatorMock) {
            client.post("/oppgave/sok") {
                autentisert()
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"fnr": $testIdent}""",
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgaver =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        object : TypeReference<List<OppgaveDTO>>() {},
                    )
                oppgaver.size shouldBe 2
            }
        }
    }

    /*
    @Test
    fun `kall uten saksbehandlingsADgruppe i claims returnerer 401`() {
        medSikretBehandlingApi {
            val tokenUtenSaksbehandlerGruppe = testAzureAdToken(ADGrupper = emptyList())

            val response =
                autentisert(
                    token = tokenUtenSaksbehandlerGruppe,
                    endepunkt = "/behandling",
                    body = """{"ident":"$ident"}""",
                )
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }*/

    private fun withOppgaveApi(
        mediator: Mediator = mockk<Mediator>(relaxed = true),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application { oppgaveApi(mediator) }
            test()
        }
    }

    private fun HttpRequestBuilder.autentisert(token: String = gyldigToken) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun lagTestOppgaveMedTilstand(tilstand: Oppgave.Tilstand.Type): Oppgave {
        return Oppgave(
            oppgaveId = UUIDv7.ny(),
            ident = "12345612345",
            emneknagger = setOf("Søknadsbehandling"),
            opprettet = ZonedDateTime.now(),
            behandlingId = UUIDv7.ny(),
            tilstand = tilstand,
        )
    }

    private fun testOppgaveFerdigBehandlet(
        oppgaveId: UUID,
        opprettet: ZonedDateTime = ZonedDateTime.now(),
    ): Oppgave {
        val opplysninger = listOf(
            Opplysning(
                navn = MINSTEINNTEKT_OPPLYSNING_NAVN,
                verdi = "true",
                dataType = "boolean",
                status = OpplysningStatus.Faktum,
            ),
        )
        return Oppgave(
            oppgaveId = oppgaveId,
            ident = "12345612345",
            emneknagger = setOf("Søknadsbehandling"),
            opprettet = opprettet,
            behandlingId = UUIDv7.ny(),
            tilstand = Oppgave.Tilstand.Type.FERDIG_BEHANDLET,
        ).also {
            it.steg.add(
                MinsteInntektSteg(opplysninger),
            )
        }
    }

    private infix fun OppgaveDTO.sammenlign(expected: OppgaveDTO) {
        this.oppgaveId shouldBe expected.oppgaveId
        this.behandlingId shouldBe expected.behandlingId
        this.personIdent shouldBe expected.personIdent
        // Vi er kun interresert i at tidspunktet er lik, uavhengig av tidssone
        this.tidspunktOpprettet.isEqual(expected.tidspunktOpprettet) shouldBe true
        this.emneknagger shouldBe expected.emneknagger
        this.tilstand shouldBe expected.tilstand
        this.steg shouldBe expected.steg
        this.journalpostIder shouldBe expected.journalpostIder
    }
}
