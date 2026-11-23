package no.nav.dagpenger.saksbehandling.innsending

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class EmneknaggForSkjemaKodeTest {
    @Test
    fun `skal lage emneknagg ut i fra skjemakode`() {
        val testKontekst: Set<Pair<String, String>> =
            setOf(
                "T6" to "Permitteringsvarsel",
                "T3" to "Tjenestebevis",
                "T4" to "Oppholdstillatelse",
                "NAV 04-08.03" to "NAV 04-08.03",
                "NAV 04-08.04" to "NAV 04-08.04",
                "NAV 90-00.08 A" to "Klage/anke",
                "NAV 90-00.08" to "Klage/anke",
                "NAV 04-06.05" to "Tar utdanning",
                "NAV 04-06.08" to "Etablering",
                "NAV 04-02.05" to "EØS",
                "NAV 04-02.01" to "EØS",
                "NAV 04-13.01" to "Grensearbeider",
                "XY" to "Faguttalelse",
            )

        testKontekst.forEach {
            val skjemaKode = it.first
            val emneknagg = it.second
            innsendingMottattHendelse(skjemaKode).emneknaggForSkjemaKode() shouldBe setOf(emneknagg)
        }
    }

    private fun innsendingMottattHendelse(skjemaKode: String): InnsendingMottattHendelse =
        InnsendingMottattHendelse(
            ident = "12345678901",
            journalpostId = "111",
            registrertTidspunkt = LocalDateTime.now(),
            søknadId = UUID.randomUUID(),
            skjemaKode = skjemaKode,
            kategori = Kategori.NY_SØKNAD,
            utførtAv = Applikasjon.DpMottak,
        )
}
