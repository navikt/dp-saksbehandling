package no.nav.dagpenger.behandling.hendelser

import io.kotest.matchers.maps.shouldContainAll
import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSporing
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.Steg
import no.nav.dagpenger.behandling.Svar
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SøknadBehandletHendelseTest {
    @Test
    fun `Skal lage søknad_behandlet_hendelse`() {
        val dato = LocalDate.of(2023, 2, 1)
        val behandling = Behandling(
            Person("12345678910"),
            testHendelse,
            setOf(
                Steg.Fastsettelse("virkningsdato", Svar(dato, LocalDate::class.java, testSporing)),
            ),
        )
        val søknadBehandletHendelse = SøknadBehandletHendelse(
            behandling = behandling,
            innvilget = true,
        )
        val expectedJsonMessageMap = mapOf(
            "behandlingId" to behandling.uuid,
            "virkningsdato" to "2023-02-01",
            "innvilget" to true,
            "ident" to behandling.person.ident,
        )

        søknadBehandletHendelse.toJsonMessageMap() shouldContainAll expectedJsonMessageMap
    }
}
