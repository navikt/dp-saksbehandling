package no.nav.dagpenger.saksbehandling.sak

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresRepository
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SakMediatorTest {
    private val testIdent = "12345678901"
    private val søknadId = UUID.randomUUID()
    private val meldekortId = 123L
    private val behandlingIdSøknad = UUID.randomUUID()
    private val behandlingIdMeldekort = UUID.randomUUID()
    private val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.8")
    private val søknadsbehandlingOpprettetHendelse =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = behandlingIdSøknad,
            ident = testIdent,
            opprettet = opprettet,
        )
    private val meldekortbehandlingOpprettetHendelse =
        MeldekortbehandlingOpprettetHendelse(
            meldekortId = meldekortId,
            behandlingId = behandlingIdMeldekort,
            ident = testIdent,
            opprettet = opprettet,
        )

    private val oppslagMock: Oppslag =
        mockk<Oppslag>(relaxed = false).also {
            coEvery { it.erSkjermetPerson(testIdent) } returns false
            coEvery { it.erAdressebeskyttetPerson(testIdent) } returns AdressebeskyttelseGradering.UGRADERT
        }

    @Test
    fun `Skal opprette sak ved mottak av søknadsbehandlingOpprettetHendelse`() {
        withMigratedDb { ds ->

            val sakMediator =
                SakMediator(
                    sakRepository = PostgresRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                )

            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelse)
            sakMediator.hentSakHistorikk(søknadsbehandlingOpprettetHendelse.ident).let {
                it.person.ident shouldBe testIdent
                it.saker().single().let { sak ->
                    sak.søknadId shouldBe søknadId
                    sak.opprettet shouldBe opprettet
                    sak.behandlinger().single().behandlingId shouldBe behandlingIdSøknad
                }
            }
        }
    }

    @Test
    fun `Skal knytte meldekortbehandling til sak ved mottak av meldekortbehandlingOpprettetHendelse`() {
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    sakRepository = PostgresRepository(ds),
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(ds),
                            oppslag = oppslagMock,
                        ),
                )
            sakMediator.opprettSak(søknadsbehandlingOpprettetHendelse)
            sakMediator.knyttTilSak(meldekortbehandlingOpprettetHendelse)

            sakMediator.hentSakHistorikk(testIdent).saker().single().behandlinger().let { behandlinger ->
                behandlinger.size shouldBe 2
                behandlinger.last().behandlingId shouldBe behandlingIdMeldekort
            }
        }
    }
}
