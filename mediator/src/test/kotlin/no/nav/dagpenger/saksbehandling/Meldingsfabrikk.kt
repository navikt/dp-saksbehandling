package no.nav.dagpenger.saksbehandling

import java.time.LocalDate
import java.util.UUID

object Meldingsfabrikk {
    val testIdent = "13083826694"
    val testSøknadUUID = UUID.randomUUID()

    internal fun innsendingFerdigstiltHendelse(
        søknadId: UUID = testSøknadUUID,
        journalpostId: String,
        type: String = "NySøknad",
        ident: String,
        datoRegistert: LocalDate = LocalDate.now(),
    ): String =
        //language=JSON
        """
        {
          "journalpostId": "$journalpostId",
          "type": "$type",
          "fødselsnummer": "$ident",
          "søknadsData": {
            "søknad_uuid": "$søknadId"
          },
          "datoRegistrert": "${datoRegistert.atStartOfDay()}",
          "@event_name": "innsending_ferdigstilt"
        } 
        """.trimIndent()
}
