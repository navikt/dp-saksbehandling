package no.nav.dagpenger.behandling

import java.util.UUID

object Meldingsfabrikk {

    //language=json
    internal fun `innsending ferdigstilt hendelse`(
        søknadId: UUID,
        journalpostId: String,
        type: String,
        ident: String
    ): String = //language=JSON
        """
        {
          "journalpostId": "$journalpostId",
          "type": "$type",
          "fødselsnummer": "$ident",
          "søknadsData": {
            "søknad_uuid": "$søknadId"
          },
          "@event_name": "innsending_ferdigstilt"
        } 
        """.trimIndent()

    internal fun aldersbehovLøsning(
        behandlingId: String = "a9586759-b71b-4295-a077-89a86453b020",
        ident: String = "12345678901"
    ): String =
        //language=JSON
        """
        {
          "@event_name": "behov",
          "@behovId": "0c773480-7f92-4d96-8824-9edbcbb91f26",
          "@behov": [
            "Aldersbehov"
          ],
          "ident": "$ident",
          "behandlingId": "$behandlingId",
          "Aldersbehov": {},
          "@id": "908cbae7-5d54-4d74-8f31-8f16109ac925",
          "@opprettet": "2022-12-05T14:02:35.564435",
          "system_read_count": 1,
          "system_participating_services": [
            {
              "id": "6e68f5ac-5654-4455-b413-ccaf72dba065",
              "time": "2022-12-05T12:31:33.428378500"
            },
            {
              "id": "6e68f5ac-5654-4455-b413-ccaf72dba065",
              "time": "2022-12-05T14:02:35.532342"
            },
            {
              "id": "908cbae7-5d54-4d74-8f31-8f16109ac925",
              "time": "2022-12-05T14:02:35.564435"
            }
          ],
          "@løsning": {
            "Aldersbehov": true
          },
          "@forårsaket_av": {
            "id": "6e68f5ac-5654-4455-b413-ccaf72dba065",
            "opprettet": "2022-12-05T12:31:33.4283785",
            "event_name": "behov",
            "behov": [
              "Aldersbehov"
            ]
          }
        }
        
        """.trimIndent()
}
