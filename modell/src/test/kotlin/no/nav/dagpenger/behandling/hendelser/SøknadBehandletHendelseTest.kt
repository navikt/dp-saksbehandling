package no.nav.dagpenger.behandling.hendelser

import io.kotest.assertions.json.shouldEqualJson
import no.nav.dagpenger.behandling.Behandling
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
            person = Person("123"),
            steg = setOf(
                Steg.Fastsettelse("virkningsdato", Svar(dato, LocalDate::class.java)),
            ),
        )
        val søknadBehandletHendelse = SøknadBehandletHendelse(
            behandling = behandling,
            innvilget = true,
        )

        //language=JSON
        val expectedJson = """{
        "@event_name": "søknad_behandlet_hendelse",
        "behandlingId": "${behandling.uuid}",
        "virkningsdato": "2023-02-01",
        "innvilget": true,
        "ident" : "${behandling.person.ident}"
    }
        """.trimIndent()

        søknadBehandletHendelse.toJson() shouldEqualJson expectedJson
    }
}
