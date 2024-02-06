package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object Meldingsfabrikk {
    val testSak = Sak(UUID.randomUUID())
    val testIdent = "13083826694"
    val testPerson = Person.rehydrer(testIdent, setOf(testSak))
    val testSøknadUUID = UUID.randomUUID()
    val søknadInnsendtHendelse =
        SøknadInnsendtHendelse(
            søknadId = testSøknadUUID,
            journalpostId = "jp",
            ident = testIdent,
            innsendtDato = LocalDate.now(),
        )

    val testSporing get() = ManuellSporing(LocalDateTime.now(), Saksbehandler("123"), "")

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
