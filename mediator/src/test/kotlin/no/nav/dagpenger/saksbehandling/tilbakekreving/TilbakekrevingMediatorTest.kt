package no.nav.dagpenger.saksbehandling.tilbakekreving

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.ManglendeTilgangTilAdressebeskyttelse
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.opprettetNå
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.testPerson
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.tilbakekreving.Tilbakekreving.BehandlingStatus
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TilbakekrevingMediatorTest {
    @Test
    fun `Skal kunne håndtere tilbakekreving hendelse`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)
        val gradertPerson = testPerson.copy(adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val saksbehandlerMedTilgane =
            TestHelper.saksbehandler.copy(
                tilganger =
                    setOf(
                        TilgangType.SAKSBEHANDLER,
                        TilgangType.STRENGT_FORTROLIG_ADRESSE,
                    ),
            )

        val tilbakekrevingBehandlingId = UUIDv7.ny()
        val tilbakekrevingBehandling =
            Behandling(
                behandlingId = tilbakekrevingBehandlingId,
                utløstAv = HendelseBehandler.Intern.Tilbakekreving,
                opprettet = opprettetNå,
                hendelse = TomHendelse,
            )
        DBTestHelper.withBehandling(
            person = gradertPerson,
            behandling = tilbakekrevingBehandling,
        ) { ds ->
            val databaseSession = DatabaseSession(ds)
            val mediator =
                TilbakekrevingMediator(
                    oppgaveMediator = oppgaveMediatorMock,
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(databaseSession = databaseSession),
                            oppslag = mockk(relaxed = true),
                        ),
                    tilbakekrevingRepository = PostgresTilbakekrevingRepository(databaseSession = databaseSession),
                    transaksjoner = Transaksjoner(databaseSession = databaseSession),
                )

            val tilbakekrevingHendelse =
                lagTilbakekrevingHendelse(
                    eksternBehandlingId = UUID.randomUUID(),
                    tilbakekrevingBehandlingId = tilbakekrevingBehandlingId,
                    status = BehandlingStatus.TIL_FORHÅNDSVARSEL,
                )

            mediator.håndter(tilbakekrevingHendelse)

            mediator
                .hent(
                    behandlingId = tilbakekrevingBehandlingId,
                    saksbehandler = saksbehandlerMedTilgane,
                ).let {
                    it.tilbakekreving shouldBe tilbakekrevingHendelse.tilbakekreving
                    it.personIdent shouldBe gradertPerson.ident
                }

            shouldThrow<ManglendeTilgangTilAdressebeskyttelse> {
                mediator.hent(
                    behandlingId = tilbakekrevingBehandlingId,
                    saksbehandler = TestHelper.saksbehandler,
                )
            }
        }
    }

    private fun lagTilbakekrevingHendelse(
        eksternBehandlingId: UUID,
        tilbakekrevingBehandlingId: UUID,
        status: BehandlingStatus,
        avventBehandlingTilDato: LocalDate? = null,
    ) = TilbakekrevingHendelse(
        eksternBehandlingId = eksternBehandlingId,
        hendelseOpprettet = LocalDateTime.now(),
        tilbakekreving =
            Tilbakekreving(
                behandlingId = tilbakekrevingBehandlingId,
                opprettet = LocalDateTime.now(),
                avventBehandlingTilDato = avventBehandlingTilDato,
                varselSendt = LocalDate.now(),
                behandlingsstatus = status,
                forrigeBehandlingsstatus = null,
                totaltFeilutbetaltBeløp = BigDecimal("25000"),
                saksbehandlingURL = "https://tilbakekreving.intern.nav.no/behandling/$tilbakekrevingBehandlingId",
                fullstendigPeriode =
                    Tilbakekreving.Periode(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 6, 30),
                    ),
            ),
    )
}
