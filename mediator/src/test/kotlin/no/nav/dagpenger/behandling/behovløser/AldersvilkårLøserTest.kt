package no.nav.dagpenger.behandling.behovløser

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AldersvilkårLøserTest {
    private val rapid = TestRapid().apply { AldersvilkårLøser(this) }

    @Test
    fun `Kan løse aldersvilkår`() {
        rapid.sendTestMessage(aldersvilkårbehov())
        assertEquals(1, rapid.inspektør.size)
        assertNotNull(rapid.inspektør.field(0, "@løsning"))
        assertTrue(rapid.inspektør.field(0, "@løsning")["Aldersbehov"].asBoolean())
    }
//language=JSON
    private fun aldersvilkårbehov() =
        """{
  "@event_name": "behov",
  "@behovId": "0c773480-7f92-4d96-8824-9edbcbb91f26",
  "@behov": [
    "Aldersbehov"
  ],
  "ident": "12345678901",
  "behandlingId": "a9586759-b71b-4295-a077-89a86453b020",
  "Aldersbehov": {},
  "@id": "6e68f5ac-5654-4455-b413-ccaf72dba065",
  "@opprettet": "2022-12-05T12:31:33.4283785",
  "system_read_count": 0,
  "system_participating_services": [
    {
      "id": "6e68f5ac-5654-4455-b413-ccaf72dba065",
      "time": "2022-12-05T12:31:33.428378500"
    }
  ]
}"""
}
