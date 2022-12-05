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
}
