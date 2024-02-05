package no.nav.dagpenger.behandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behandling.Mediator
import no.nav.dagpenger.behandling.modell.hendelser.VurderAvslagPåMinsteinntektHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.UUID

class VurderMinsteinntektAvslagMottakTest {
    private val testRapid = TestRapid()
    private val mediator = mockk<Mediator>(relaxed = true)
    val søknadId = UUID.randomUUID()
    val ident = "1234567890"
    private val vurderAvslagPåMinsteinntektHendelse =
        VurderAvslagPåMinsteinntektHendelse(
            ident = ident,
            søknadId = søknadId,
        )

    init {
        VurderMinsteinntektAvslagMottak(testRapid, mediator)
    }

    @Test
    fun `Skal behandle manuell_behandling hendelser grunnet mulig gjenopptak`() {
        testRapid.sendTestMessage(testMessageMuligGjenopptak)
        verify(exactly = 1) {
            mediator.behandle(vurderAvslagPåMinsteinntektHendelse)
        }
    }

    @Test
    fun `Skal ikke behandle manuell_behandling hendelser med ugyldig grunn`() {
        testRapid.sendTestMessage(testMessageUgyldigGrunn)
        verify(exactly = 0) {
            mediator.behandle(vurderAvslagPåMinsteinntektHendelse)
        }
    }

    @Test
    fun `Skal ikke behandle manuell_behandling hendelser som mangler seksjon_navn`() {
        testRapid.sendTestMessage(testMessageManglerSeksjonNavn)
        verify(exactly = 0) {
            mediator.behandle(vurderAvslagPåMinsteinntektHendelse)
        }
    }

    @Language("JSON")
    private val testMessageMuligGjenopptak =
        """
{
    "@event_name": "manuell_behandling",
    "@opprettet": "2024-01-30T10:43:32.988331190",
    "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
    "søknad_uuid": "$søknadId",
    "seksjon_navn": "mulig gjenopptak",
    "fakta": [{
            "id": "33",
            "navn": "Har brukt opp forrige dagpengeperiode"
        }
    ],
    "identer": [{
            "id": "$ident",
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

    @Language("JSON")
    private val testMessageUgyldigGrunn =
        """
{
    "@event_name": "manuell_behandling",
    "@opprettet": "2024-01-30T10:43:32.988331190",
    "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
    "søknad_uuid": "f97f30f2-90ec-4236-a43a-b43cdf82c656",
    "seksjon_navn": "ugyldig grunn",
    "fakta": [{
            "id": "-2",
            "navn": "tull og tøys"
        }
    ],
    "identer": [{
            "id": "01010155555",
            "type": "folkeregisterident",
            "historisk": false
        }
    ],
    "-1": true,
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

    @Language("JSON")
    private val testMessageManglerSeksjonNavn =
        """
{
    "@event_name": "manuell_behandling",
    "@opprettet": "2024-01-30T10:43:32.988331190",
    "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
    "søknad_uuid": "f97f30f2-90ec-4236-a43a-b43cdf82c656",
    "fakta": [{
            "id": "-5",
            "navn": "tull og tøys"
        }
    ],
    "identer": [{
            "id": "01010155555",
            "type": "folkeregisterident",
            "historisk": false
        }
    ],
    "-4": true,
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
