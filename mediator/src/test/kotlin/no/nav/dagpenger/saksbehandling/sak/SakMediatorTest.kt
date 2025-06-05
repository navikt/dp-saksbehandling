package no.nav.dagpenger.saksbehandling.sak

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.db.sak.InmemoryRepository
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SakMediatorTest {
    private val testIdent = "12345678901"
    private val søknadId = UUID.randomUUID()
    private val meldekortId = 123L
    private val behandlingIdSøknad = UUID.randomUUID()
    private val behandlingIdMeldekort = UUID.randomUUID()
    private val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.800935377")
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

    @BeforeEach
    fun settOppTest() {
        InmemoryRepository.reset()
    }

    @Test
    fun `Skal opprette sak ved mottak av søknadsbehandlingOpprettetHendelse`() {
        val sakMediator =
            SakMediator(
                personRepository = InmemoryRepository,
                sakRepository = InmemoryRepository,
            )

        sakMediator.opprettSak(søknadsbehandlingOpprettetHendelse)

        val person = InmemoryRepository.hent(testIdent)
        person.ident shouldBe testIdent
        person.saker().size shouldBe 1
        person.saker().single().let { sak ->
            sak.søknadId shouldBe søknadId
            sak.opprettet shouldBe opprettet
            sak.behandlinger.single().behandlingId shouldBe behandlingIdSøknad
        }
    }

    @Test
    fun `Skal knytte meldekortbehandling til sak ved mottak av meldekortbehandlingOpprettetHendelse`() {
        val sakMediator =
            SakMediator(
                personRepository = InmemoryRepository,
                sakRepository = InmemoryRepository,
            )
        sakMediator.opprettSak(søknadsbehandlingOpprettetHendelse)
        sakMediator.knyttTilSak(meldekortbehandlingOpprettetHendelse)

        InmemoryRepository.hent(testIdent).saker().single().behandlinger.let { behandlinger ->
            behandlinger.size shouldBe 2
            behandlinger.last().behandlingId shouldBe meldekortId
        }
    }
}
