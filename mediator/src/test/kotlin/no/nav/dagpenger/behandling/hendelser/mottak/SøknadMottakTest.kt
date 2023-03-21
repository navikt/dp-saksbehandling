package no.nav.dagpenger.behandling.hendelser.mottak

import no.nav.dagpenger.behandling.Meldingsfabrikk.`innsending ferdigstilt hendelse`
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class SøknadMottakTest {
    @ParameterizedTest
    @ValueSource(strings = ["NySøknad"])
    fun `Skal behandle innsending_ferdigstilt event for type NySøknad`(type: String) {
        TestRapid().let { testRapid ->
            SøknadMottak(
                testRapid,
            )
            val søknadId = UUID.randomUUID()
            val journalpostId = "jp1"
            val ident = "ident1"

            testRapid.sendTestMessage(
                `innsending ferdigstilt hendelse`(
                    søknadId = søknadId,
                    journalpostId = journalpostId,
                    type = type,
                    ident = ident,
                ),
            )
        }
    }
}
