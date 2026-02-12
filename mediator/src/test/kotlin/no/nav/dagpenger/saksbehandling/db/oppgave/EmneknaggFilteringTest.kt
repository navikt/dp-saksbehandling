package no.nav.dagpenger.saksbehandling.db.oppgave

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.hentEmneknaggKategori
import org.junit.jupiter.api.Test

class EmneknaggFilteringTest {
    @Test
    fun `Gruppering av emneknagger etter kategori`() {
        val emneknagger =
            setOf(
                Emneknagg.Regelknagg.AVSLAG.visningsnavn,
                Emneknagg.Regelknagg.INNVILGELSE.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_ORDINÆR.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT.visningsnavn,
                Emneknagg.Regelknagg.GJENOPPTAK.visningsnavn,
            )

        val gruppert = emneknagger.grupperEmneknaggPerKategori()

        gruppert.size shouldBe 3
        gruppert["SØKNADSRESULTAT"] shouldBe
            setOf(
                Emneknagg.Regelknagg.AVSLAG.visningsnavn,
                Emneknagg.Regelknagg.INNVILGELSE.visningsnavn,
            )
        gruppert["RETTIGHET"] shouldBe
            setOf(
                Emneknagg.Regelknagg.RETTIGHET_ORDINÆR.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT.visningsnavn,
            )
        gruppert["GJENOPPTAK"] shouldBe setOf(Emneknagg.Regelknagg.GJENOPPTAK.visningsnavn)
    }

    @Test
    fun `Avslagsgrunner grupperes sammen`() {
        val emneknagger =
            setOf(
                Emneknagg.Regelknagg.AVSLAG_MINSTEINNTEKT.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_ARBEIDSTID.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_ALDER.visningsnavn,
            )

        val gruppert = emneknagger.grupperEmneknaggPerKategori()

        gruppert.size shouldBe 1
        gruppert["AVSLAGSGRUNN"]?.size shouldBe 3
    }

    @Test
    fun `PåVent og Avbrutt grupperes riktig`() {
        val emneknagger =
            setOf(
                Emneknagg.PåVent.AVVENT_SVAR.visningsnavn,
                Emneknagg.PåVent.AVVENT_DOKUMENTASJON.visningsnavn,
                Emneknagg.AvbrytBehandling.AVBRUTT_ANNET.visningsnavn,
            )

        val gruppert = emneknagger.grupperEmneknaggPerKategori()

        gruppert.size shouldBe 2
        gruppert["PÅ_VENT"]?.size shouldBe 2
        gruppert["AVBRUTT_GRUNN"]?.size shouldBe 1
    }

    @Test
    fun `Ettersending får egen kategori`() {
        val emneknagger =
            setOf(
                "Ettersending(2024-01-15)",
                "Ettersending(2024-02-20)",
            )

        val gruppert = emneknagger.grupperEmneknaggPerKategori()

        gruppert.size shouldBe 1
        gruppert["ETTERSENDING"]?.size shouldBe 2
    }

    @Test
    fun `Ukjente emneknagger får UDEFINERT kategori`() {
        val emneknagger =
            setOf(
                "Ukjent knagg 1",
                "Ukjent knagg 2",
            )

        val gruppert = emneknagger.grupperEmneknaggPerKategori()

        gruppert.size shouldBe 1
        gruppert["UDEFINERT"]?.size shouldBe 2
    }

    @Test
    fun `Tom liste gir tomt map`() {
        val emneknagger = emptySet<String>()

        val gruppert = emneknagger.grupperEmneknaggPerKategori()

        gruppert shouldBe emptyMap()
    }

    @Test
    fun `Mikset liste grupperes korrekt`() {
        val emneknagger =
            setOf(
                Emneknagg.Regelknagg.AVSLAG.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_ORDINÆR.visningsnavn,
                Emneknagg.PåVent.AVVENT_SVAR.visningsnavn,
                "Ettersending(2024-01-15)",
                "Ukjent",
            )

        val gruppert = emneknagger.grupperEmneknaggPerKategori()

        gruppert.size shouldBe 5
        gruppert["SØKNADSRESULTAT"]?.size shouldBe 1
        gruppert["RETTIGHET"]?.size shouldBe 1
        gruppert["PÅ_VENT"]?.size shouldBe 1
        gruppert["ETTERSENDING"]?.size shouldBe 1
        gruppert["UDEFINERT"]?.size shouldBe 1
    }
}

private fun Set<String>.grupperEmneknaggPerKategori(): Map<String, Set<String>> =
    this
        .groupBy { visningsNavn ->
            hentEmneknaggKategori(visningsNavn).name
        }.mapValues { it.value.toSet() }
