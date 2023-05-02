package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.Hendelse
import java.util.UUID

object Meldingsfabrikk {
    val testIdent = "12312312311"
    val testPerson = Person(testIdent)
    val testHendelse = object : Hendelse(testIdent) {}

    //language=json
    internal fun `innsending ferdigstilt hendelse`(
        søknadId: UUID,
        journalpostId: String,
        type: String,
        ident: String,
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
