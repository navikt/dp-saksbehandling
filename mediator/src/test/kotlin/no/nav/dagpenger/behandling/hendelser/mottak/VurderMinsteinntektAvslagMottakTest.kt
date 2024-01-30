package no.nav.dagpenger.behandling.hendelser.mottak

import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class VurderMinsteinntektAvslagMottakTest {
    private val testRapid = TestRapid()

    init {
        VurderMinsteinntektAvslagMottak(testRapid, mockk())
    }

    @Test
    fun `Skal behandle manuell_behandling hendelser grunnet mulig gjenopptak`() {
        testRapid.sendTestMessage(testMessageMuligGjenopptak)

        // TODO
        // hent oppgave basert på søknad-id
        // insert oppgave_emneknagg for vurderAvslagPåMinsteinntekt
    }

    @Language("JSON")
    private val testMessageMuligGjenopptak =
        """
{
    "@event_name": "manuell_behandling",
    "@opprettet": "2024-01-30T10:43:32.988331190",
    "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
    "søknad_uuid": "f97f30f2-90ec-4236-a43a-b43cdf82c656",
    "seksjon_navn": "mulig gjenopptak",
    "fakta": [{
            "id": "33",
            "navn": "Har brukt opp forrige dagpengeperiode"
        }
    ],
    "identer": [{
            "id": "01010155555",
            "type": "folkeregisterident",
            "historisk": false
        }, {
            "id": "1000098693185",
            "type": "aktørid",
            "historisk": false
        }
    ],
    "32": true,
    "system_read_count": 0,
    "system_participating_services": [{
            "id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "time": "2024-01-30T10:43:32.988718812",
            "service": "dp-quiz-mediator",
            "instance": "dp-quiz-mediator-5b77fd98f4-nwzt8",
            "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-quiz:2024.01.29-12.56-3012aef"
        }
    ]
}
        """
}
