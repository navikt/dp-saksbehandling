package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.PersonHendelse
import java.time.LocalDateTime
import java.util.UUID

object Meldingsfabrikk {
    val testSak = Sak(UUID.randomUUID())
    val testIdent = "12312312311"
    val testPerson = Person.rehydrer(testIdent, setOf(testSak))
    val testHendelse = object : PersonHendelse(UUID.randomUUID(), testIdent) {}
    val testSporing get() = ManuellSporing(LocalDateTime.now(), Saksbehandler("123"), "")

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
