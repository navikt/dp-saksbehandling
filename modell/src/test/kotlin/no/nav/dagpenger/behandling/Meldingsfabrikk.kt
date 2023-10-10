package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.PersonHendelse
import java.time.LocalDateTime
import java.util.UUID

object Meldingsfabrikk {
    val testIdent = "12312312311"
    val testPerson = Person(testIdent)
    val testHendelse = object : PersonHendelse(UUID.randomUUID(), testIdent) {}

    fun testSporing(roller: List<Rolle>) = ManuellSporing(LocalDateTime.now(), Saksbehandler("123", roller), "")
}
