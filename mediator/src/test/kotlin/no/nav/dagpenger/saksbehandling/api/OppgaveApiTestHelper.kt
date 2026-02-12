package no.nav.dagpenger.saksbehandling.api

import PersonMediator
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag

internal object OppgaveApiTestHelper {
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
                    produksjonsstatistikkRepository = mockk(relaxed = true),
                    klageMediator = mockk(relaxed = true),
                    klageDTOMapper = mockk(relaxed = true),
                    personMediator = personMediator,
                    sakMediator = mockk(relaxed = true),
                    innsendingMediator = mockk(relaxed = true),
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
                    oppgaveDTOMapper =
                        OppgaveDTOMapper(
                            Oppslag(
                                pdlKlient = pdlKlient,
                                relevanteJournalpostIdOppslag = relevanteJournalpostIdOppslag,
                                saksbehandlerOppslag = saksbehandlerOppslag,
                                skjermingKlient = mockk(relaxed = true),
                            ),
                            OppgaveHistorikkDTOMapper(oppgaveRepository, saksbehandlerOppslag),
                            mockk<SakMediator>(relaxed = true),
                        ),
                    produksjonsstatistikkRepository = mockk(relaxed = true),
                    klageMediator = mockk(relaxed = true),
                    klageDTOMapper = mockk(relaxed = true),
                    personMediator = personMediator,
                    sakMediator = mockk(relaxed = true),
                    innsendingMediator = mockk(relaxed = true),
                )
            }
            test()
        }
    }
}
