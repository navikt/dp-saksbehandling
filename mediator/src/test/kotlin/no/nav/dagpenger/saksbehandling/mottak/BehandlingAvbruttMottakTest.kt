package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.UUID

class BehandlingAvbruttMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)
    private val ident = "09830698334"
    private val behandlingId = UUID.fromString("018ec271-6a29-7fcc-95df-37d48118072f")
    private val behandletHendelseId = UUID.fromString("a830499b-5bcd-4401-9db4-8e54549e9e0f")

    init {
        BehandlingAvbruttMottak(rapidsConnection = testRapid, oppgaveMediator = oppgaveMediatorMock)
    }

    @ParameterizedTest
    @CsvSource(
        "Søknad, true",
        "Manuell, true",
        "Meldekort, true",
        "Brevdue, false",
    )
    fun `Skal behandle BehandlingAvbruttHendelse for gitte hendelsetyper`(
        hendelseType: String,
        skalBehandles: Boolean,
    ) {
        testRapid.sendTestMessage(behandlingAvbruttHendelse(hendelseType = hendelseType))
        when (skalBehandles) {
            true -> {
                verify(exactly = 1) {
                    oppgaveMediatorMock.avbrytOppgave(
                        BehandlingAvbruttHendelse(
                            behandlingId = behandlingId,
                            behandletHendelseId = behandletHendelseId.toString(),
                            behandletHendelseType = hendelseType,
                            ident = ident,
                        ),
                    )
                }
            }

            false -> {
                verify(exactly = 0) {
                    oppgaveMediatorMock.avbrytOppgave(
                        BehandlingAvbruttHendelse(
                            behandlingId = behandlingId,
                            behandletHendelseId = behandletHendelseId.toString(),
                            behandletHendelseType = hendelseType,
                            ident = ident,
                        ),
                    )
                }
            }
        }
    }

    //language=json
    fun behandlingAvbruttHendelse(hendelseType: String) =
        """
        {
          "@event_name": "behandling_avbrutt",
          "ident": "$ident",
          "behandlingId": "$behandlingId",
          "gjelderDato": "2024-04-09",
          "fagsakId": "0",
          "behandletHendelse": {
                "datatype": "UUID",
                "id": "$behandletHendelseId",
                "type": "$hendelseType"
              },
          "@id": "7333f08e-dfeb-438e-aba3-9cd6387fca73",
          "@opprettet": "2024-04-10T10:00:21.081950694",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "7333f08e-dfeb-438e-aba3-9cd6387fca73",
              "time": "2024-04-10T10:00:21.081950694",
              "service": "dp-behandling",
              "instance": "dp-behandling-86599cc6d5-lp5kh",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-behandling:2024.04.10-07.51-74cf1b5"
            }
          ]
        }
        """.trimIndent()
}
