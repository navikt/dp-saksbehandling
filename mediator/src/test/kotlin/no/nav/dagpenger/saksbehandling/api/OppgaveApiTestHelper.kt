package no.nav.dagpenger.saksbehandling.api

import PersonMediator
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag

internal object OppgaveApiTestHelper {
    private val mockAzure = mockAzure()

    fun withOppgaveApi(
        oppgaveMediator: OppgaveMediator = mockk<OppgaveMediator>(relaxed = true),
        oppgaveDTOMapper: OppgaveDTOMapper = mockk<OppgaveDTOMapper>(relaxed = true),
        personMediator: PersonMediator = mockk(relaxed = true),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application {
                installerApis(
                    oppgaveMediator = oppgaveMediator,
                    oppgaveDTOMapper = oppgaveDTOMapper,
                    statistikkTjeneste = mockk(relaxed = true),
                    klageMediator = mockk(relaxed = true),
                    klageDTOMapper = mockk(relaxed = true),
                    personMediator = personMediator,
                    sakMediator = mockk(relaxed = true),
                )
            }
            test()
        }
    }

    fun withOppgaveApi(
        oppgaveMediator: OppgaveMediator = mockk<OppgaveMediator>(relaxed = true),
        pdlKlient: PDLKlient = mockk(relaxed = true),
        relevanteJournalpostIdOppslag: RelevanteJournalpostIdOppslag = mockk(relaxed = true),
        saksbehandlerOppslag: SaksbehandlerOppslag = mockk(relaxed = true),
        oppgaveRepository: OppgaveRepository = mockk<OppgaveRepository>(relaxed = true),
        personMediator: PersonMediator = mockk(relaxed = true),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application {
                installerApis(
                    oppgaveMediator = oppgaveMediator,
                    oppgaveDTOMapper = OppgaveDTOMapper(
                        Oppslag(
                            pdlKlient = pdlKlient,
                            relevanteJournalpostIdOppslag = relevanteJournalpostIdOppslag,
                            saksbehandlerOppslag = saksbehandlerOppslag,
                            skjermingKlient = mockk(relaxed = true),
                        ),
                        OppgaveHistorikkDTOMapper(oppgaveRepository, saksbehandlerOppslag),
                        mockk<SakMediator>(relaxed = true),
                    ),
                    statistikkTjeneste = mockk(relaxed = true),
                    klageMediator = mockk(relaxed = true),
                    klageDTOMapper = mockk(relaxed = true),
                    personMediator = personMediator,
                    sakMediator = mockk(relaxed = true),
                )
            }
            test()
        }
    }

    fun HttpRequestBuilder.autentisert(token: String = gyldigSaksbehandlerToken()) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    fun gyldigSaksbehandlerToken(
        adGrupper: List<String> = emptyList(),
        navIdent: String = TestHelper.saksbehandler.navIdent,
    ): String {
        return mockAzure.lagTokenMedClaims(
            mapOf(
                "groups" to listOf("SaksbehandlerADGruppe") + adGrupper,
                "NAVident" to navIdent,
            ),
        )
    }

    fun gyldigMaskinToken(): String = mockAzure.lagTokenMedClaims(mapOf("idtyp" to "app"))
}
