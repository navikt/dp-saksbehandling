package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_UTSENDING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.db.DataNotFoundException
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.helper.distribuertDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.DistribusjonFerdigstiltMottak
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilVedtakMottak
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class OppgaveMediatorTest {
    private val testIdent = "12345612345"
    private val sak = Sak("12342", "Arena")
    private val testRapid = TestRapid()
    private val pdlKlientMock = mockk<PDLKlient>(relaxed = true)
    private val skjermingKlientMock = mockk<SkjermingKlient>(relaxed = true)
    private val emneknagger = setOf("EØSArbeid", "SykepengerSiste36Måneder")

    @Test
    fun `Skal ignorere ForslagTilVedtakHendelse hvis oppgave ikke finnes for den behandlingen`() {
        withMigratedDb { datasource ->
            val repo = PostgresOppgaveRepository(datasource)
            val oppgaveMediator = OppgaveMediator(repo, testRapid)
            ForslagTilVedtakMottak(rapidsConnection = testRapid, oppgaveMediator = oppgaveMediator)

            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = "ad",
                    søknadId = UUIDv7.ny(),
                    behandlingId = UUIDv7.ny(),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
            oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).size shouldBe 0
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir vedtatt`() {
        withMigratedDb { datasource ->
            val oppgaveMediator =
                OppgaveMediator(repository = PostgresOppgaveRepository(datasource), rapidsConnection = testRapid)

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, skjermingKlientMock, pdlKlientMock)
            DistribusjonFerdigstiltMottak(oppgaveMediator, testRapid)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()
            val søknadsbehandlingOpprettetHendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = LocalDateTime.now(),
                )

            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.hentAlleOppgaverMedTilstand(OPPRETTET).let { oppgaver ->
                oppgaver.size shouldBe 1
                oppgaver.single().behandling.hendelse shouldBe søknadsbehandlingOpprettetHendelse
            }

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            )

            val oppgaverKlarTilBehandling = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)

            oppgaverKlarTilBehandling.size shouldBe 1
            val oppgave = oppgaverKlarTilBehandling.single()
            oppgave.behandlingId shouldBe behandlingId
            oppgave.emneknagger shouldContainAll emneknagger

            oppgaveMediator.tildelOppgave(
                OppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    navIdent = "NAVIdent",
                ),
            )

            val tildeltOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
            tildeltOppgave.tilstand().type shouldBe UNDER_BEHANDLING
            tildeltOppgave.saksbehandlerIdent shouldBe "NAVIdent"

            val vedtakFattetHendelse =
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = testIdent,
                    sak = sak,
                )

            oppgaveMediator.startUtsending(vedtakFattetHendelse)
            val oppgaveMedUtsending = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
            oppgaveMedUtsending.tilstand().type shouldBe AVVENTER_UTSENDING

            testRapid.inspektør.size shouldBe 1
            testRapid.inspektør.message(0).let { jsonNode ->
                jsonNode["@event_name"].asText() shouldBe "start_utsending"
                jsonNode["oppgaveId"].asUUID() shouldBe oppgave.oppgaveId
                jsonNode["behandlingId"].asUUID() shouldBe oppgave.behandlingId
                jsonNode["ident"].asText() shouldBe oppgave.ident
                jsonNode["sak"].let { sakIdNode ->
                    sakIdNode["id"].asText() shouldBe sak.id
                    sakIdNode["kontekst"].asText() shouldBe sak.kontekst
                }
            }

            testRapid.sendTestMessage(
                distribuertDokumentBehovLøsning(
                    oppgaveId = oppgave.oppgaveId,
                    journalpostId = "mikkemus",
                    distribusjonId = "distId",
                ),
            )
            val ferdigstiltOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
            ferdigstiltOppgave.tilstand().type shouldBe FERDIG_BEHANDLET
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir avbrutt`() {
        withMigratedDb { datasource ->
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(datasource),
                    rapidsConnection = testRapid,
                )

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, skjermingKlientMock, pdlKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()

            oppgaveMediator.opprettOppgaveForBehandling(
                søknadsbehandlingOpprettetHendelse =
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                        ident = testIdent,
                        opprettet = LocalDateTime.now(),
                    ),
            )
            oppgaveMediator.hentAlleOppgaverMedTilstand(OPPRETTET).size shouldBe 1

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(ident = testIdent, søknadId = søknadId, behandlingId = behandlingId),
            )
            val oppgaver = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
            oppgaver.size shouldBe 1
            oppgaver.single().behandlingId shouldBe behandlingId

            oppgaveMediator.avbrytOppgave(
                BehandlingAvbruttHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = testIdent,
                ),
            )

            assertThrows<DataNotFoundException> {
                oppgaveMediator.hentOppgave(oppgaver.single().oppgaveId)
            }
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir satt på vent`() {
        withMigratedDb { datasource ->
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(datasource),
                    rapidsConnection = testRapid,
                )

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, skjermingKlientMock, pdlKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()

            oppgaveMediator.opprettOppgaveForBehandling(
                søknadsbehandlingOpprettetHendelse =
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                        ident = testIdent,
                        opprettet = LocalDateTime.now(),
                    ),
            )
            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(ident = testIdent, søknadId = søknadId, behandlingId = behandlingId),
            )

            val oppgave = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).single()

            oppgaveMediator.tildelOppgave(
                OppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    navIdent = "NAVIdent",
                ),
            )

            val utSattTil = LocalDate.now().plusDays(17)
            oppgaveMediator.utsettOppgave(
                UtsettOppgaveHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    navIdent = "NAVIdent",
                    utsattTil = utSattTil,
                    beholdOppgave = false,
                ),
            )

            oppgaveMediator.hentAlleOppgaverMedTilstand(PAA_VENT).size shouldBe 1
        }
    }
}
