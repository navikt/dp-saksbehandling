package no.nav.dagpenger.saksbehandling.serder

import io.kotest.assertions.json.shouldEqualJson
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class HendelseJsonSerDerTest {
    private val aUUID = "018fa4fe-8450-7fa2-9e47-cafa81f718cd"
    private val søknadsbehandlingOpprettetHendelse =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = UUID.fromString(aUUID),
            behandlingId = UUID.fromString(aUUID),
            ident = "ident",
            opprettet = LocalDateTime.MIN.truncatedTo(ChronoUnit.HOURS),
        )

    private val søknadsbehandlingOpprettetHendelseJson =
        """
            { 
               "søknadId": "$aUUID",
               "behandlingId": "$aUUID",
               "ident": "ident",
               "opprettet": "-999999999-01-01T00:00:00",
               "utførtAv": {
                  "navn": "dp-saksbehandling"
               }
           }
            """

    @Test
    fun `should serialize hendelse to json`() {
        TomHendelse.tilJson() shouldEqualJson "{}"
        val tilJson = søknadsbehandlingOpprettetHendelse.tilJson()
        tilJson shouldEqualJson søknadsbehandlingOpprettetHendelseJson
    }
}
