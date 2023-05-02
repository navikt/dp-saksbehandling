package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.Hendelse

object Meldingsfabrikk {
    val testIdent = "12312312311"
    val testPerson = Person(testIdent)
    val testHendelse = object : Hendelse(testIdent) {}
}
