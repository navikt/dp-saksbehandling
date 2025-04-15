package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.OpplysningerVerdi
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.autentisert
import no.nav.dagpenger.saksbehandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageOppgave
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KlageRouteTest {
    init {
        mockAzure()
    }

    private val klageId = UUIDv7.ny()
    private val opplysningId = UUIDv7.ny()

    @Test
    fun `skal kaste feil når det mangler autentisering`() {
        val mediator = mockk<KlageMediator>()
        withKlageRoute(mediator) {
            client.get("oppgave//klage/$klageId").let { response ->
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    @Test
    fun `Skal hente klageDTO`() {
        val klageOppgaveId = UUIDv7.ny()
        val mediator =
            mockk<KlageMediator>().also {
                every { it.hentKlageOppgave(klageOppgaveId) } returns
                    KlageOppgave(
                        oppgaveId = klageOppgaveId,
                        opprettet = LocalDateTime.now(),
                        journalpostId = null,
                        klageBehandling =
                            KlageBehandling(
                                person =
                                    Person(
                                        id = UUID.randomUUID(),
                                        ident = "12345678901",
                                        skjermesSomEgneAnsatte = false,
                                        adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                                    ),
                            ),
                    )
            }
        withKlageRoute(mediator) {
            client.get("oppgave/klage/$klageOppgaveId") { autentisert() }.let {
                it.status shouldBe HttpStatusCode.OK
            }
            // todo mer testing
        }
    }

    @Test
    fun `Skal kunne oppdatere en  opplysning av type flervalg `() {
        val tekstListe = OpplysningerVerdi.TekstListe("tekst1", "tekst2")
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageId, opplysningId, tekstListe)
                } returns Unit
            }
        withKlageRoute(mediator) {
            client.put("oppgave/klage/$klageId/opplysning/$opplysningId") {
                autentisert()
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi" : [ "tekst1", "tekst2" ], "type" : "FLER_LISTEVALG" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        klageOppgaveId = klageId,
                        opplysningId = opplysningId,
                        verdi = tekstListe,
                    )
                }
            }
        }
    }

    @Test
    fun `Skal kunne oppdatere en opplysning av type tekst `() {
        val tekst = OpplysningerVerdi.Tekst("tekst")
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageId, opplysningId, tekst)
                } returns Unit
            }
        withKlageRoute(mediator) {
            client.put("oppgave/klage/$klageId/opplysning/$opplysningId") {
                autentisert()
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi" : "tekst", "type" : "TEKST" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        klageOppgaveId = klageId,
                        opplysningId = opplysningId,
                        verdi = tekst,
                    )
                }
            }
        }
    }

    @Test
    fun `Skal kunne oppdatere en opplysning av type boolean `() {
        val boolsk = OpplysningerVerdi.Boolsk(false)
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageId, opplysningId, boolsk)
                } returns Unit
            }
        withKlageRoute(mediator) {
            client.put("oppgave/klage/$klageId/opplysning/$opplysningId") {
                autentisert()
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi" : ${boolsk.value}, "type" : "BOOLSK" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        klageOppgaveId = klageId,
                        opplysningId = opplysningId,
                        verdi = boolsk,
                    )
                }
            }
        }
    }

    @Test
    fun `Skal kunne oppdatere en opplysning av type dato `() {
        val dato = OpplysningerVerdi.Dato(LocalDate.of(2021, 1, 1))
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageId, opplysningId, dato)
                } returns Unit
            }
        withKlageRoute(mediator) {
            client.put("oppgave/klage/$klageId/opplysning/$opplysningId") {
                autentisert()
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi" : "2021-01-01", "type" : "DATO" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        klageOppgaveId = klageId,
                        opplysningId = opplysningId,
                        verdi = dato,
                    )
                }
            }
        }
    }

    private fun withKlageRoute(
        klageMediator: KlageMediator,
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            this.application {
                installerApis(mockk(), mockk(), mockk(), klageMediator)
            }
            test()
        }
    }
}

fun main() {
    objectMapper.writeValueAsString(BoolskVerdiDTO(true)).let { println(it) }
}
