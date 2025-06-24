package no.nav.dagpenger.saksbehandling.serder

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
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

    private fun behandlingOppretteHendelse(utførtAv: Behandler): BehandlingOpprettetHendelse {
        val uuid = UUID.fromString(aUUID)
        return BehandlingOpprettetHendelse(
            behandlingId = uuid,
            ident = "1234",
            sakId = uuid,
            opprettet = LocalDateTime.MIN.truncatedTo(ChronoUnit.HOURS),
            type = BehandlingType.RETT_TIL_DAGPENGER,
            utførtAv = utførtAv,
        )
    }

    private val søknadsbehandlingOpprettetHendelseJson =
        """
            { 
               "søknadId": "$aUUID",
               "behandlingId": "$aUUID",
               "ident": "ident",
               "opprettet": "-999999999-01-01T00:00:00",
               "utførtAv": { "navn": "dp-behandling" }
            }
            """

    @Test
    fun `Kan serialisere og deserialisere hendelser til og fra json`() {
        behandlingOppretteHendelse(utførtAv = Applikasjon("dp-mottak")).let { hendelse ->
            val json = hendelse.tilJson()
            json shouldEqualJson
                //language=Json
                """
             { 
                "behandlingId": "$aUUID",
                "ident": "1234",
                "sakId": "${hendelse.sakId}",
                "opprettet": "-999999999-01-01T00:00:00",
                "type": "RETT_TIL_DAGPENGER",
                "utførtAv": { "navn": "dp-mottak" }
             }
            """
            json.tilHendelse<BehandlingOpprettetHendelse>() shouldBe hendelse
        }

        behandlingOppretteHendelse(
            utførtAv =
                Saksbehandler(
                    navIdent = "navIdent",
                    grupper = setOf("gruppe1", "gruppe2"),
                    tilganger = setOf(TilgangType.BESLUTTER, TilgangType.SAKSBEHANDLER),
                ),
        ).let { hendelse ->
            val json = hendelse.tilJson()
            json shouldEqualJson
                //language=Json
                """
             { 
                "behandlingId": "$aUUID",
                "ident": "1234",
                "opprettet": "-999999999-01-01T00:00:00",
                "type": "RETT_TIL_DAGPENGER",
                "sakId": "$aUUID",
                "utførtAv": {
                  "navIdent": "navIdent",
                  "grupper": [
                    "gruppe1",
                    "gruppe2"
                  ],
                  "tilganger": [
                    "BESLUTTER",
                    "SAKSBEHANDLER"
                  ]
                }              
             }
            """
            json.tilHendelse<BehandlingOpprettetHendelse>() shouldBe hendelse
        }
    }

    @Test
    fun `should serialize hendelse to json`() {
        TomHendelse.tilJson() shouldEqualJson "{}"
        val tilJson = søknadsbehandlingOpprettetHendelse.tilJson()
        tilJson shouldEqualJson søknadsbehandlingOpprettetHendelseJson
    }

    @Test
    fun `should deserialize hendelse to json`() {
        """{}""".tilHendelse<TomHendelse>() shouldBe TomHendelse
        søknadsbehandlingOpprettetHendelseJson.tilHendelse<SøknadsbehandlingOpprettetHendelse>() shouldBe søknadsbehandlingOpprettetHendelse
    }
}
