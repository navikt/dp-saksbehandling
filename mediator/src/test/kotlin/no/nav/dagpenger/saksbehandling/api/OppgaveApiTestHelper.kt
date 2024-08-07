package no.nav.dagpenger.saksbehandling.api

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.FerdigBehandlet
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.Opprettet
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.config.apiConfig
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import java.time.LocalDate
import java.time.LocalDateTime

internal object OppgaveApiTestHelper {
    const val TEST_IDENT = "12345612345"
    const val TEST_NAV_IDENT = "Z999999"
    private val mockAzure = mockAzure()
    private val fødselsdato = LocalDate.of(2000, 1, 1)

    fun withOppgaveApi(
        oppgaveMediator: OppgaveMediator = mockk<OppgaveMediator>(relaxed = true),
        pdlKlient: PDLKlient = mockk(relaxed = true),
        journalpostIdClient: JournalpostIdClient = mockk(relaxed = true),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application {
                apiConfig()
                oppgaveApi(oppgaveMediator, pdlKlient, journalpostIdClient)
            }
            test()
        }
    }

    fun HttpRequestBuilder.autentisert(token: String = gyldigSaksbehandlerToken()) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    fun gyldigSaksbehandlerToken(): String =
        mockAzure.lagTokenMedClaims(
            mapOf(
                "groups" to listOf("SaksbehandlerADGruppe"),
                "NAVident" to TEST_NAV_IDENT,
            ),
        )

    fun gyldigSaksbehandlerMedTilgangTilEgneAnsatteToken(): String =
        mockAzure.lagTokenMedClaims(
            mapOf(
                "groups" to listOf("SaksbehandlerADGruppe", "EgneAnsatteADGruppe"),
                "NAVident" to TEST_NAV_IDENT,
            ),
        )

    fun lagMediatorMock(): OppgaveMediator {
        return mockk<OppgaveMediator>().also {
            every { it.personSkjermesSomEgneAnsatte(any()) } returns false
        }
    }

    fun lagTestOppgaveMedTilstandOgBehandling(
        tilstand: Oppgave.Tilstand.Type,
        saksbehandlerIdent: String? = null,
        behandling: Behandling,
        utsattTil: LocalDate? = null,
    ): Oppgave {
        return Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            ident = TEST_IDENT,
            saksbehandlerIdent = saksbehandlerIdent,
            behandlingId = behandling.behandlingId,
            opprettet = LocalDateTime.now(),
            emneknagger = setOf("Søknadsbehandling"),
            tilstand =
                when (tilstand) {
                    OPPRETTET -> Opprettet
                    KLAR_TIL_BEHANDLING -> KlarTilBehandling
                    UNDER_BEHANDLING -> UnderBehandling
                    FERDIG_BEHANDLET -> FerdigBehandlet
                    PAA_VENT -> TODO()
                },
            behandling = behandling,
            utsattTil = utsattTil,
        )
    }

    fun lagTestOppgaveMedTilstand(
        tilstand: Oppgave.Tilstand.Type,
        saksbehandlerIdent: String? = null,
        skjermesSomEgneAnsatte: Boolean = false,
        utsattTil: LocalDate? = null,
    ): Oppgave {
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                person =
                    Person(
                        id = UUIDv7.ny(),
                        ident = TEST_IDENT,
                        skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
                        adresseBeskyttelseGradering = UGRADERT,
                    ),
                opprettet = LocalDateTime.now(),
            )
        return lagTestOppgaveMedTilstandOgBehandling(tilstand, saksbehandlerIdent, behandling, utsattTil)
    }

    val testPerson =
        PDLPersonIntern(
            ident = TEST_IDENT,
            fornavn = "PETTER",
            etternavn = "SMART",
            mellomnavn = null,
            fødselsdato = fødselsdato,
            alder = 0,
            statsborgerskap = "NOR",
            kjønn = PDLPerson.Kjonn.UKJENT,
            adresseBeskyttelseGradering = UGRADERT,
        )
}
