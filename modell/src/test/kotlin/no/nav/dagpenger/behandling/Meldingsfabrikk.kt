package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.PersonHendelse
import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import java.time.LocalDateTime
import java.util.UUID

object Meldingsfabrikk {
    val testIdent = "12312312311"
    val testPerson = Person(testIdent)
    val testHendelse = object : PersonHendelse(UUID.randomUUID(), testIdent) {}
    val testSporing get() = ManuellSporing(LocalDateTime.now(), Saksbehandler("123"), "")
}
