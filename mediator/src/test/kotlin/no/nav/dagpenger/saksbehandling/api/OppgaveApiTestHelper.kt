package no.nav.dagpenger.saksbehandling.api

import PersonMediator
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.SAKSBEHANDLER_IDENT
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.defaultSaksbehandlerADGruppe
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag

internal object OppgaveApiTestHelper {
//    const val TEST_IDENT = "12345612345"
//    val TEST_UUID = UUIDv7.ny()
// //    const val SAKSBEHANDLER_IDENT = "SaksbehandlerIdent"
// //    val defaultSaksbehandlerADGruppe = listOf("SaksbehandlerADGruppe")
// //    const val BESLUTTER_IDENT = "BeslutterIdent"
//    val SOKNAD_ID = "01953789-f215-744e-9f6e-a55509bae78b".toUUID()
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
                    oppgaveMediator,
                    oppgaveDTOMapper,
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    mockk(relaxed = true),
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
                    oppgaveMediator,
                    OppgaveDTOMapper(
                        Oppslag(
                            pdlKlient,
                            relevanteJournalpostIdOppslag,
                            saksbehandlerOppslag,
                            skjermingKlient = mockk(relaxed = true),
                        ),
                        OppgaveHistorikkDTOMapper(oppgaveRepository, saksbehandlerOppslag),
                        mockk<SakMediator>(relaxed = true),
                    ),
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    mockk(relaxed = true),
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
        navIdent: String = SAKSBEHANDLER_IDENT,
    ): String {
        return mockAzure.lagTokenMedClaims(
            mapOf(
                "groups" to defaultSaksbehandlerADGruppe + adGrupper,
                "NAVident" to navIdent,
            ),
        )
    }

    fun gyldigMaskinToken(): String = mockAzure.lagTokenMedClaims(mapOf("idtyp" to "app"))
}
