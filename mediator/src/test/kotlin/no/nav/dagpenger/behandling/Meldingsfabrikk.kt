package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import java.time.LocalDateTime
import java.util.UUID

object Meldingsfabrikk {
    val testSak = Sak(UUID.randomUUID())
    val testIdent = "12312312311"
    val testPerson = Person.rehydrer(testIdent, setOf(testSak))
    val søknadInnsendtHendelse =
        SøknadInnsendtHendelse(søknadId = UUID.randomUUID(), journalpostId = "jp", ident = testIdent)

    val testSporing get() = ManuellSporing(LocalDateTime.now(), Saksbehandler("123"), "")

    internal fun innsendingFerdigstiltHendelse(
        søknadId: UUID,
        journalpostId: String,
        type: String,
        ident: String,
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
          "@event_name": "innsending_ferdigstilt"
        } 
        """.trimIndent()
}
