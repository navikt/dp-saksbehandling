package no.nav.dagpenger.saksbehandling.db.sak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource

class PostgresSakRepositoryTest {
    private val nå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    private val person = DBTestHelper.testPerson

    private val oppgaveId = UUIDv7.ny()
    private val søknadIdSak1 = UUIDv7.ny()
    private val behandlingId1iSak1 = UUIDv7.ny()
    private val behandling1iSak1 =
        Behandling(
            behandlingId = behandlingId1iSak1,
            utløstAv = UtløstAvType.SØKNAD,
            opprettet = nå.minusDays(9),
            oppgaveId = oppgaveId,
            hendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadIdSak1,
                    behandlingId = behandlingId1iSak1,
                    ident = DBTestHelper.testPerson.ident,
                    opprettet = nå.minusDays(9),
                    basertPåBehandling = null,
                ),
        )
    private val behandling2iSak1 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = UtløstAvType.SØKNAD,
            opprettet = nå.minusDays(5),
            hendelse = TomHendelse,
        )

    private val søknadId1Sak2 = UUIDv7.ny()
    private val søknadId2Sak2 = UUIDv7.ny()
    private val behandlingId1iSak2 = UUIDv7.ny()
    private val behandlingId2iSak2 = UUIDv7.ny()
    private val behandling1iSak2 =
        Behandling(
            behandlingId = behandlingId1iSak2,
            utløstAv = UtløstAvType.SØKNAD,
            opprettet = nå.minusDays(1),
            hendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId1Sak2,
                    behandlingId = behandlingId1iSak1,
                    ident = DBTestHelper.testPerson.ident,
                    opprettet = nå.minusDays(1),
                    basertPåBehandling = null,
                ),
        )
    private val behandling2iSak2 =
        Behandling(
            behandlingId = behandlingId2iSak2,
            utløstAv = UtløstAvType.SØKNAD,
            opprettet = nå.minusDays(3),
            hendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId2Sak2,
                    behandlingId = behandlingId2iSak2,
                    ident = DBTestHelper.testPerson.ident,
                    opprettet = nå.minusDays(3),
                    basertPåBehandling = behandlingId1iSak2,
                ),
        )
    private val sak1 =
        Sak(
            søknadId = søknadIdSak1,
            opprettet = nå,
        ).also {
            it.leggTilBehandling(behandling1iSak1)
            it.leggTilBehandling(behandling2iSak1)
        }
    private val sak2 =
        Sak(
            søknadId = søknadId1Sak2,
            opprettet = nå,
        ).also {
            // Emulerer out of order lesing
            it.leggTilBehandling(behandling2iSak2)
            it.leggTilBehandling(behandling1iSak2)
        }
    private val sakHistorikk =
        SakHistorikk(
            person = person,
        ).also {
            it.leggTilSak(sak1)
            it.leggTilSak(sak2)
        }

    @Test
    fun `Skal kunne lagre sakHistorikk`() {
        DBTestHelper.withPerson(person) { dataSource ->
            val sakRepository = PostgresSakRepository(dataSource = dataSource)
            sakRepository.lagre(sakHistorikk)
            this.leggTilOppgave(oppgaveId, behandling1iSak1.behandlingId)
            val sakHistorikkFraDB = sakRepository.hentSakHistorikk(person.ident)

            sakHistorikkFraDB shouldBe sakHistorikk

            // Sjekker at saker og behandling blir sortert kronologisk, med nyeste først
            sakHistorikkFraDB
                .saker()
                .first()
                .behandlinger()
                .first()
                .behandlingId shouldBe behandling2iSak2.behandlingId
        }
    }

    @Test
    fun `Hent sakId basert på behandlingId`() {
        DBTestHelper.withSaker(saker = listOf(sak1)) { ds ->
            val sakRepository = PostgresSakRepository(ds)

            sakRepository.merkSakenSomDpSak(sak1.sakId, true)
            sakRepository.hentSakIdForBehandlingId(behandling1iSak1.behandlingId) shouldBe sak1.sakId
            sakRepository.hentDagpengerSakIdForBehandlingId(behandling1iSak1.behandlingId) shouldBe sak1.sakId

            sakRepository.merkSakenSomDpSak(sak1.sakId, false)
            sakRepository.hentSakIdForBehandlingId(behandling1iSak1.behandlingId) shouldBe sak1.sakId
            shouldThrow<DataNotFoundException> {
                sakRepository.hentDagpengerSakIdForBehandlingId(behandling1iSak1.behandlingId) shouldBe sak1.sakId
            }
        }
    }

    @Test
    fun `Henter sakId til nyeste dp-sak for en person`() {
        DBTestHelper.withSaker(saker = listOf(sak1, sak2)) { ds ->
            val sakRepository = PostgresSakRepository(ds)

            ds.opprettOppgaveForBehandling(behandlingId = behandling1iSak1.behandlingId)
            ds.opprettOppgaveForBehandling(behandlingId = behandling2iSak1.behandlingId)
            ds.opprettOppgaveForBehandling(behandlingId = behandling1iSak2.behandlingId)
            ds.opprettOppgaveForBehandling(behandlingId = behandling2iSak2.behandlingId)

            sakRepository.finnSisteSakId(ident = person.ident) shouldBe null

            sakRepository.merkSakenSomDpSak(sakId = sak1.sakId, erDpSak = true)
            sakRepository.finnSisteSakId(ident = person.ident) shouldBe sak1.sakId

            sakRepository.merkSakenSomDpSak(sakId = sak2.sakId, erDpSak = true)
            sakRepository.finnSisteSakId(ident = person.ident) shouldBe sak2.sakId

            ds.avbrytOppgave(behandlingId = behandling2iSak2.behandlingId)
            sakRepository.finnSisteSakId(ident = person.ident) shouldBe sak2.sakId

            ds.avbrytOppgave(behandlingId = behandling1iSak2.behandlingId)
            sakRepository.finnSisteSakId(ident = person.ident) shouldBe sak1.sakId
        }
    }

    @Test
    fun `Finner sakId for en søknad`() {
        DBTestHelper.withSaker(saker = listOf(sak1, sak2)) { ds ->
            val sakRepository = PostgresSakRepository(ds)

            sakRepository.finnSakIdForSøknad(
                søknadId = sak1.søknadId,
                ident = DBTestHelper.testPerson.ident,
            ) shouldBe null

            sakRepository.merkSakenSomDpSak(sakId = sak1.sakId, erDpSak = true)
            sakRepository.finnSakIdForSøknad(
                søknadId = sak1.søknadId,
                ident = DBTestHelper.testPerson.ident,
            ) shouldBe sak1.sakId

            sakRepository.merkSakenSomDpSak(sakId = sak2.sakId, erDpSak = true)
            sakRepository.finnSakIdForSøknad(
                søknadId = sak2.søknadId,
                ident = DBTestHelper.testPerson.ident,
            ) shouldBe sak2.sakId
        }
    }

    private fun DataSource.avbrytOppgave(behandlingId: UUID) =
        sessionOf(this).use { session ->
            session.run(
                action =
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            UPDATE oppgave_v1
                            SET    tilstand = 'AVBRUTT'
                            WHERE  behandling_id = :behandling_id
                            """.trimIndent(),
                        paramMap =
                            mapOf(
                                "behandling_id" to behandlingId,
                            ),
                    ).asUpdate,
            )
        }

    private fun DataSource.opprettOppgaveForBehandling(
        behandlingId: UUID,
        tilstand: String = "KLAR_TIL_BEHANDLING",
    ) = sessionOf(this).use { session ->
        session.run(
            action =
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO oppgave_v1
                        ( id, behandling_id, opprettet, tilstand, melding_om_vedtak_kilde, kontrollert_brev )
                        VALUES
                        ( gen_random_uuid(), :behandling_id, now(), :tilstand, 'DP_SAK', 'IKKE_RELEVANT')
                        ON CONFLICT (id) DO NOTHING 
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "behandling_id" to behandlingId,
                            "tilstand" to tilstand,
                        ),
                ).asUpdate,
        )
    }
}
