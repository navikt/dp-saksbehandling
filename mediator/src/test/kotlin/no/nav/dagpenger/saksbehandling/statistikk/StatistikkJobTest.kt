package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.FailedMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers_api.SentMessage
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.TestHelper.ISO_TIMESTAMP
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.statistikk.db.SaksbehandlingsstatistikkRepository
import no.nav.dagpenger.saksbehandling.statistikk.db.SaksbehandlingsstatistikkRepository.Companion.ANTALL_PER_KJØRING
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class StatistikkJobTest {
    private val testRapid = TestRapid()
    val mottatt = LocalDateTime.of(2026, 1, 11, 9, 11, 30)
    val søknadKlarTilBehandling =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = mottatt,
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = null,
            beslutterIdent = null,
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveITilstand.Tilstandsendring(
                    sekvensnummer = 1,
                    tilstandsendringId = UUIDv7.ny(),
                    tilstand = "KLAR_TIL_BEHANDLING",
                    tidspunkt = mottatt.minusDays(1),
                ),
            utløstAv = "SØKNAD",
            behandlingResultat = null,
            fagsystem = null,
            behandlingÅrsak = null,
            arenaSakId = null,
            resultatBegrunnelse = null,
            relatertBehandlingId = null,
        )

    val søknadAvbrutt =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = mottatt,
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = "AB123",
            beslutterIdent = "B987",
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveITilstand.Tilstandsendring(
                    sekvensnummer = 2,
                    tilstandsendringId = UUIDv7.ny(),
                    tilstand = "AVBRUTT_MANUELT",
                    tidspunkt = mottatt,
                ),
            utløstAv = "SØKNAD",
            behandlingResultat = "AVBRUTT",
            behandlingÅrsak = "BEHANDLES_I_ARENA",
            fagsystem = "ARENA",
            arenaSakId = "123",
            resultatBegrunnelse = null,
            relatertBehandlingId = null,
        )

    val innsendingFerdigBehandlet =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = mottatt,
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = "SB111",
            beslutterIdent = null,
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveITilstand.Tilstandsendring(
                    sekvensnummer = 3,
                    tilstandsendringId = UUIDv7.ny(),
                    tilstand = "FERDIG_BEHANDLET",
                    tidspunkt = mottatt,
                ),
            utløstAv = "INNSENDING",
            behandlingResultat = "RettTilDagpenger",
            behandlingÅrsak = "Årsak",
            fagsystem = "DAGPENGER",
            arenaSakId = null,
            resultatBegrunnelse = null,
            relatertBehandlingId = null,
        )

    val oppgavePåVent =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = mottatt,
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = null,
            beslutterIdent = null,
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveITilstand.Tilstandsendring(
                    sekvensnummer = 4,
                    tilstandsendringId = UUIDv7.ny(),
                    tilstand = "PAA_VENT",
                    tidspunkt = mottatt.minusDays(1),
                ),
            utløstAv = "SØKNAD",
            behandlingResultat = null,
            fagsystem = null,
            behandlingÅrsak = null,
            arenaSakId = null,
            resultatBegrunnelse = "AVVENT_MELDEKORT",
            relatertBehandlingId = null,
        )

    val oppgaveTilResending =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = mottatt,
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = null,
            beslutterIdent = null,
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveITilstand.Tilstandsendring(
                    sekvensnummer = 4,
                    tilstandsendringId = UUIDv7.ny(),
                    tilstand = "PAA_VENT",
                    tidspunkt = mottatt.minusDays(1),
                ),
            utløstAv = "SØKNAD",
            behandlingResultat = null,
            fagsystem = null,
            behandlingÅrsak = null,
            arenaSakId = null,
            resultatBegrunnelse = "AVVENT_MELDEKORT",
            relatertBehandlingId = null,
        )
    val klageFerdigBehandlet =
        OppgaveITilstand(
            oppgaveId = UUIDv7.ny(),
            mottatt = mottatt,
            sakId = UUIDv7.ny(),
            behandlingId = UUIDv7.ny(),
            personIdent = "12345612345",
            saksbehandlerIdent = null,
            beslutterIdent = null,
            versjon = "dp:saksbehandling:1.2.3",
            tilstandsendring =
                OppgaveITilstand.Tilstandsendring(
                    sekvensnummer = 5,
                    tilstandsendringId = UUIDv7.ny(),
                    tilstand = "FERDIG_BEHANDLET",
                    tidspunkt = mottatt,
                ),
            utløstAv = "KLAGE",
            behandlingResultat = "OPPHEVET",
            fagsystem = "DAGPENGER",
            behandlingÅrsak = null,
            arenaSakId = null,
            resultatBegrunnelse = null,
            relatertBehandlingId = UUIDv7.ny(),
        )
    private val saksbehandlingsstatistikkRepository =
        mockk<SaksbehandlingsstatistikkRepository>().also {
            every { it.oppgaveTilstandsendringerIkkeOverfort(any()) } returns emptyList()
            every { it.oppgaveTilstandsendringer() } returns
                listOf(
                    søknadKlarTilBehandling,
                    søknadAvbrutt,
                    innsendingFerdigBehandlet,
                    oppgavePåVent,
                    klageFerdigBehandlet,
                )
            every { it.markerTilstandsendringerSomOverført(oppgaveTilResending.tilstandsendring.tilstandsendringId) } returns 1
            every { it.markerTilstandsendringerSomOverført(søknadKlarTilBehandling.tilstandsendring.tilstandsendringId) } returns 1
            every { it.markerTilstandsendringerSomOverført(søknadAvbrutt.tilstandsendring.tilstandsendringId) } returns 1
            every { it.markerTilstandsendringerSomOverført(oppgavePåVent.tilstandsendring.tilstandsendringId) } returns 1
            every { it.markerTilstandsendringerSomOverført(innsendingFerdigBehandlet.tilstandsendring.tilstandsendringId) } returns 1
            every { it.markerTilstandsendringerSomOverført(klageFerdigBehandlet.tilstandsendring.tilstandsendringId) } returns 1
        }

    @Test
    fun `Skal publisere oppgavetilstandsendringer til statistikk på riktig format og sette de som publisert`() {
        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                saksbehandlingsstatistikkRepository = saksbehandlingsstatistikkRepository,
            ).executeJob()
        }

        testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk_v7",
              "oppgave": {
                "oppgaveId": "${søknadKlarTilBehandling.oppgaveId}",
                "mottatt": "${søknadKlarTilBehandling.mottatt.format(ISO_TIMESTAMP)}",
                "sakId": "${søknadKlarTilBehandling.sakId}",
                "behandlingId": "${søknadKlarTilBehandling.behandlingId}",
                "personIdent": "12345612345",
                "tilstandsendring": {
                  "sekvensnummer": 1,
                  "tilstandsendringId": "${søknadKlarTilBehandling.tilstandsendring.tilstandsendringId}",
                  "tilstand": "KLAR_TIL_BEHANDLING",
                  "tidspunkt": "${søknadKlarTilBehandling.tilstandsendring.tidspunkt.format(ISO_TIMESTAMP)}"
                },
                "utløstAv": "SØKNAD",
                "versjon": "dp:saksbehandling:1.2.3"
              }
            }
            """.trimIndent()
        testRapid.inspektør.message(1).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk_v7",
              "oppgave": {
                "oppgaveId": "${søknadAvbrutt.oppgaveId}",
                "mottatt": "${søknadAvbrutt.mottatt.format(ISO_TIMESTAMP)}",
                "sakId": "${søknadAvbrutt.sakId}",
                "behandlingId": "${søknadAvbrutt.behandlingId}",
                "personIdent": "12345612345",
                "saksbehandlerIdent": "AB123",
                "beslutterIdent": "B987",
                "tilstandsendring": {
                  "sekvensnummer": 2,
                  "tilstandsendringId": "${søknadAvbrutt.tilstandsendring.tilstandsendringId}",
                  "tilstand": "AVBRUTT_MANUELT",
                  "tidspunkt": "${søknadAvbrutt.tilstandsendring.tidspunkt.format(ISO_TIMESTAMP)}"
                },
                "utløstAv": "SØKNAD",
                "versjon": "dp:saksbehandling:1.2.3",
                "behandlingResultat": "AVBRUTT",
                "behandlingÅrsak": "BEHANDLES_I_ARENA",
                "fagsystem": "ARENA",
                "arenaSakId": "123"
              }
            }
            """.trimIndent()
        testRapid.inspektør.message(2).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk_v7",
              "oppgave": {
                "oppgaveId": "${innsendingFerdigBehandlet.oppgaveId}",
                "mottatt": "${innsendingFerdigBehandlet.mottatt.format(ISO_TIMESTAMP)}",
                "sakId": "${innsendingFerdigBehandlet.sakId}",
                "behandlingId": "${innsendingFerdigBehandlet.behandlingId}",
                "personIdent": "12345612345",
                "saksbehandlerIdent": "SB111",
                "tilstandsendring": {
                  "sekvensnummer": 3,
                  "tilstandsendringId": "${innsendingFerdigBehandlet.tilstandsendring.tilstandsendringId}",
                  "tilstand": "FERDIG_BEHANDLET",
                  "tidspunkt": "${innsendingFerdigBehandlet.tilstandsendring.tidspunkt.format(ISO_TIMESTAMP)}"
                },
                "utløstAv": "INNSENDING",
                "versjon": "dp:saksbehandling:1.2.3",
                "behandlingResultat": "RettTilDagpenger"
              }
            }
            """.trimIndent()
        testRapid.inspektør.message(3).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk_v7",
              "oppgave": {
                "oppgaveId": "${oppgavePåVent.oppgaveId}",
                "mottatt": "${oppgavePåVent.mottatt.format(ISO_TIMESTAMP)}",
                "sakId": "${oppgavePåVent.sakId}",
                "behandlingId": "${oppgavePåVent.behandlingId}",
                "personIdent": "12345612345",
                "tilstandsendring": {
                  "sekvensnummer": 4,
                  "tilstandsendringId": "${oppgavePåVent.tilstandsendring.tilstandsendringId}",
                  "tilstand": "PAA_VENT",
                  "tidspunkt": "${oppgavePåVent.tilstandsendring.tidspunkt.format(ISO_TIMESTAMP)}"
                },
                "utløstAv": "SØKNAD",
                "versjon": "dp:saksbehandling:1.2.3",
                "resultatBegrunnelse": "AVVENT_MELDEKORT"
              }
            }
            """.trimIndent()
        testRapid.inspektør.message(4).toString() shouldEqualSpecifiedJsonIgnoringOrder
            """
            {
              "@event_name": "oppgave_til_statistikk_v7",
              "oppgave": {
                "oppgaveId": "${klageFerdigBehandlet.oppgaveId}",
                "mottatt": "${klageFerdigBehandlet.mottatt.format(ISO_TIMESTAMP)}",
                "sakId": "${klageFerdigBehandlet.sakId}",
                "behandlingId": "${klageFerdigBehandlet.behandlingId}",
                "personIdent": "12345612345",
                "tilstandsendring": {
                  "sekvensnummer": 5,
                  "tilstandsendringId": "${klageFerdigBehandlet.tilstandsendring.tilstandsendringId}",
                  "tilstand": "FERDIG_BEHANDLET",
                  "tidspunkt": "${klageFerdigBehandlet.tilstandsendring.tidspunkt.format(ISO_TIMESTAMP)}"
                },
                "utløstAv": "KLAGE",
                "versjon": "dp:saksbehandling:1.2.3",
                "relatertBehandlingId": "${klageFerdigBehandlet.relatertBehandlingId}",
                "behandlingResultat": "OPPHEVET"
              }
            }
            """.trimIndent()
    }

    @Test
    fun `Skal ikke markere tilstandsendring som overført ved leveransefeil og skal stoppe videre publisering`() {
        // Feiler på melding nr 3 (innsendingFerdigBehandlet) med FailedMessage uten å kaste
        val rapidMedLeveransefeil = FeilendePåIndeksRapid(feilPåIndeks = 3, delegate = testRapid)

        shouldThrow<RuntimeException> {
            runBlocking {
                StatistikkJob(
                    rapidsConnection = rapidMedLeveransefeil,
                    saksbehandlingsstatistikkRepository = saksbehandlingsstatistikkRepository,
                ).executeJob()
            }
        }

        // De to første ble levert og markert
        verify(exactly = 1) {
            saksbehandlingsstatistikkRepository.markerTilstandsendringerSomOverført(
                søknadKlarTilBehandling.tilstandsendring.tilstandsendringId,
            )
        }
        verify(exactly = 1) {
            saksbehandlingsstatistikkRepository.markerTilstandsendringerSomOverført(
                søknadAvbrutt.tilstandsendring.tilstandsendringId,
            )
        }
        // Den feilede og de etterfølgende skal IKKE markeres (ellers stille hull i statistikken)
        verify(exactly = 0) {
            saksbehandlingsstatistikkRepository.markerTilstandsendringerSomOverført(
                innsendingFerdigBehandlet.tilstandsendring.tilstandsendringId,
            )
            saksbehandlingsstatistikkRepository.markerTilstandsendringerSomOverført(oppgavePåVent.tilstandsendring.tilstandsendringId)
        }
        verify(exactly = 0) {
            saksbehandlingsstatistikkRepository.markerTilstandsendringerSomOverført(
                klageFerdigBehandlet.tilstandsendring.tilstandsendringId,
            )
        }
    }

    @Test
    fun `Skal ikke skrive nye statistikkrader så lenge det finnes uleverte rader`() {
        every {
            saksbehandlingsstatistikkRepository.oppgaveTilstandsendringerIkkeOverfort(any())
        } returns listOf(oppgaveTilResending)

        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                saksbehandlingsstatistikkRepository = saksbehandlingsstatistikkRepository,
            ).executeJob()
        }

        // Uten denne gaten ville backloggen vokst med én batch per kjøring så lenge Kafka er nede.
        verify(exactly = 0) { saksbehandlingsstatistikkRepository.oppgaveTilstandsendringer() }

        testRapid.inspektør.size shouldBe 1
        verify(exactly = 1) {
            saksbehandlingsstatistikkRepository.markerTilstandsendringerSomOverført(
                oppgaveTilResending.tilstandsendring.tilstandsendringId,
            )
        }
    }

    @Test
    fun `Skal skrive nye statistikkrader når det ikke finnes uleverte rader`() {
        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                saksbehandlingsstatistikkRepository = saksbehandlingsstatistikkRepository,
            ).executeJob()
        }

        verify(exactly = 1) { saksbehandlingsstatistikkRepository.oppgaveTilstandsendringer() }
        testRapid.inspektør.size shouldBe 5
    }

    @Test
    fun `Duplikate statistikkrader for samme tilstandsendring skal ikke stoppe eksporten`() {
        every {
            saksbehandlingsstatistikkRepository.markerTilstandsendringerSomOverført(
                søknadKlarTilBehandling.tilstandsendring.tilstandsendringId,
            )
        } returns 2

        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                saksbehandlingsstatistikkRepository = saksbehandlingsstatistikkRepository,
            ).executeJob()
        }

        testRapid.inspektør.size shouldBe 5
    }

    @Test
    fun `Skal stoppe når ingen rad ble markert som overført`() {
        every {
            saksbehandlingsstatistikkRepository.markerTilstandsendringerSomOverført(
                søknadKlarTilBehandling.tilstandsendring.tilstandsendringId,
            )
        } returns 0

        shouldThrow<IllegalStateException> {
            runBlocking {
                StatistikkJob(
                    rapidsConnection = testRapid,
                    saksbehandlingsstatistikkRepository = saksbehandlingsstatistikkRepository,
                ).executeJob()
            }
        }

        testRapid.inspektør.size shouldBe 1
        verify(exactly = 0) {
            saksbehandlingsstatistikkRepository.markerTilstandsendringerSomOverført(
                søknadAvbrutt.tilstandsendring.tilstandsendringId,
            )
        }
    }

    @Test
    fun `Skal hente uleverte rader med samme batchstørrelse som skrivingen`() {
        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                saksbehandlingsstatistikkRepository = saksbehandlingsstatistikkRepository,
            ).executeJob()
        }

        verify(exactly = 1) {
            saksbehandlingsstatistikkRepository.oppgaveTilstandsendringerIkkeOverfort(ANTALL_PER_KJØRING)
        }
    }
}

private class FeilendePåIndeksRapid(
    private val feilPåIndeks: Int,
    private val delegate: RapidsConnection,
) : RapidsConnection() {
    private var antallPublisert = 0

    override fun publish(message: String) = TODO("ikke i bruk")

    override fun publish(
        key: String,
        message: String,
    ) = TODO("ikke i bruk")

    override fun publish(messages: List<OutgoingMessage>): Pair<List<SentMessage>, List<FailedMessage>> {
        antallPublisert++
        return if (antallPublisert == feilPåIndeks) {
            emptyList<SentMessage>() to
                messages.mapIndexed { index, melding ->
                    FailedMessage(index, melding, RuntimeException("Simulert leveransefeil"))
                }
        } else {
            delegate.publish(messages)
        }
    }

    override fun rapidName(): String = "FeilendePåIndeksRapid"

    override fun start() {}

    override fun stop() {}
}
