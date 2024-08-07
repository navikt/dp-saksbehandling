package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.autentisert
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigSaksbehandlerMedTilgangTilEgneAnsatteToken
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigSaksbehandlerToken
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.lagTestOppgaveMedTilstand
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.testPerson
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.withOppgaveApi
import no.nav.dagpenger.saksbehandling.api.mockAzure
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveApiTilgangskontrollTest {
    private val mockAzure = mockAzure()

    @Test
    fun `Skal avvise kall uten autoriserte AD grupper`() {
        withOppgaveApi {
            client.get("/oppgave") { autentisert(token = mockAzure.lagTokenMedClaims(mapOf("groups" to "UgyldigADGruppe"))) }
                .status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Avvis kall til oppgaver som gjelder egne ansatte dersom saksbehandler ikke har riktig adgruppe`() {
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)

        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also { oppgaveMediator ->
                every { oppgaveMediator.personSkjermesSomEgneAnsatte(any()) } returns true
            }

        withOppgaveApi(oppgaveMediatorMock) {
            val saksbehandlerTokenUtenEgneAnsatteTilgang = gyldigSaksbehandlerToken()
            client.get("/oppgave/${testOppgave.oppgaveId}") { autentisert(token = saksbehandlerTokenUtenEgneAnsatteTilgang) }
                .status shouldBe HttpStatusCode.Forbidden

            client.put("/oppgave/${testOppgave.oppgaveId}/tildel") { autentisert(token = saksbehandlerTokenUtenEgneAnsatteTilgang) }
                .status shouldBe HttpStatusCode.Forbidden

            client.put("/oppgave/${testOppgave.oppgaveId}/utsett") { autentisert(token = saksbehandlerTokenUtenEgneAnsatteTilgang) }
                .status shouldBe HttpStatusCode.Forbidden
        }
    }

    @Test
    fun `Godta kall til oppgaver som gjelder egne ansatte dersom saksbehandler har riktig ad-gruppe`() {
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)
        val pdlMock = mockk<PDLKlient>()
        coEvery { pdlMock.person(any()) } returns Result.success(testPerson)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.personSkjermesSomEgneAnsatte(any()) } returns true
                every { it.tildelOppgave(any()) } returns testOppgave
                every { it.hentOppgave(any()) } returns testOppgave
                every { it.utsettOppgave(any()) } just Runs
            }

        withOppgaveApi(oppgaveMediatorMock, pdlMock) {
            client.get("/oppgave/${testOppgave.oppgaveId}") {
                autentisert(token = gyldigSaksbehandlerMedTilgangTilEgneAnsatteToken())
            }.status shouldBe HttpStatusCode.OK

            client.put("/oppgave/${testOppgave.oppgaveId}/tildel") {
                autentisert(token = gyldigSaksbehandlerMedTilgangTilEgneAnsatteToken())
            }.status shouldBe HttpStatusCode.OK

            client.put("/oppgave/${testOppgave.oppgaveId}/utsett") {
                autentisert(token = gyldigSaksbehandlerMedTilgangTilEgneAnsatteToken())
                contentType(Application.Json)
                setBody(
                    //language=JSON
                    """
                        {
                          "utsettTilDato":"${LocalDate.now()}",
                          "beholdOppgave":"true"
                        }
                    """.trimMargin(),
                )
            }
                .status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `Saksbehandler kan legge tilbake oppgaven dersom hen ikke lenger har rettighet til å behandle personen`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>()
        val testOppgaveForEgenAnsatt =
            lagTestOppgaveMedTilstand(
                tilstand = UNDER_BEHANDLING,
                saksbehandlerIdent = OppgaveApiTestHelper.TEST_IDENT,
                skjermesSomEgneAnsatte = true,
            )
        coEvery { oppgaveMediatorMock.hentOppgave(any()) } returns testOppgaveForEgenAnsatt
        coEvery {
            oppgaveMediatorMock.fristillOppgave(
                OppgaveAnsvarHendelse(
                    testOppgaveForEgenAnsatt.oppgaveId,
                    OppgaveApiTestHelper.TEST_NAV_IDENT,
                ),
            )
        } just runs

        withOppgaveApi(oppgaveMediatorMock, mockk<PDLKlient>()) {
            client.put("/oppgave/${testOppgaveForEgenAnsatt.oppgaveId}/legg-tilbake") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
        }
    }
}
