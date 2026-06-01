package no.nav.dagpenger.saksbehandling.api

import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.audit.Auditlogg
import no.nav.dagpenger.saksbehandling.audit.TestAuditlogg
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.db.person.PersonRepository
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag

internal object OppgaveApiTestHelper {
    val personRepositoryMock: PersonRepository =
        mockk<PersonRepository>().also {
            every { it.erNødbremset(any()) } returns false
        }

    fun withOppgaveApi(
        oppgaveMediator: OppgaveMediator = mockk<OppgaveMediator>(relaxed = true),
        oppgaveDTOMapper: OppgaveDTOMapper = mockk<OppgaveDTOMapper>(relaxed = true),
        personMediator: PersonMediator = mockk(relaxed = true),
        auditlogg: Auditlogg = TestAuditlogg(),
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
                    meldingOmVedtakMediator = mockk(relaxed = true),
                    oppfølgingMediator = mockk(relaxed = true),
                    auditlogg = auditlogg,
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
        personRepository: PersonRepository = personRepositoryMock,
        auditlogg: Auditlogg = TestAuditlogg(),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application {
                installerApis(
                    oppgaveMediator = oppgaveMediator,
                    oppgaveDTOMapper =
                        OppgaveDTOMapper(
                            oppslag =
                                Oppslag(
                                    pdlKlient = pdlKlient,
                                    relevanteJournalpostIdOppslag = relevanteJournalpostIdOppslag,
                                    saksbehandlerOppslag = saksbehandlerOppslag,
                                    skjermingKlient = mockk(relaxed = true),
                                    personRepository = personRepositoryMock,
                                ),
                            oppgaveHistorikkDTOMapper = OppgaveHistorikkDTOMapper(oppgaveRepository, saksbehandlerOppslag),
                            sakMediator = mockk<SakMediator>(relaxed = true),
                        ),
                    produksjonsstatistikkRepository = mockk(relaxed = true),
                    klageMediator = mockk(relaxed = true),
                    klageDTOMapper = mockk(relaxed = true),
                    personMediator = personMediator,
                    sakMediator = mockk(relaxed = true),
                    innsendingMediator = mockk(relaxed = true),
                    meldingOmVedtakMediator = mockk(relaxed = true),
                    oppfølgingMediator = mockk(relaxed = true),
                    auditlogg = auditlogg,
                )
            }
            test()
        }
    }
}
