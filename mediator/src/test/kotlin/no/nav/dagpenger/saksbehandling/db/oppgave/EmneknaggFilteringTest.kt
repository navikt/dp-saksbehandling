package no.nav.dagpenger.saksbehandling.db.oppgave

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Emneknagg
import org.junit.jupiter.api.Test

class EmneknaggFilteringTest {
    @Test
    fun `Gruppering av emneknagger etter kategori`() {
        val emneknagger =
            setOf(
                Emneknagg.Regelknagg.AVSLAG.visningsnavn, // SØKNADSRESULTAT
                Emneknagg.Regelknagg.INNVILGELSE.visningsnavn, // SØKNADSRESULTAT
                Emneknagg.Regelknagg.RETTIGHET_ORDINÆR.visningsnavn, // RETTIGHET
                Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT.visningsnavn, // RETTIGHET
                Emneknagg.Regelknagg.GJENOPPTAK.visningsnavn, // GJENOPPTAK
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

// Hjelpefunksjon for testing - må være private extension i produksjonskoden
private fun Set<String>.grupperEmneknaggPerKategori(): Map<String, Set<String>> =
    this
        .groupBy { visningsNavn ->
            when {
                visningsNavn.startsWith("Ettersending") -> "ETTERSENDING"
                else -> hentKategoriForEmneknagg(visningsNavn)
            }
        }.mapValues { it.value.toSet() }

private fun hentKategoriForEmneknagg(visningsNavn: String): String {
    Emneknagg.Regelknagg.entries.find { it.visningsnavn == visningsNavn }?.let { regelknagg ->
        return when (regelknagg) {
            Emneknagg.Regelknagg.AVSLAG,
            Emneknagg.Regelknagg.INNVILGELSE,
            -> "SØKNADSRESULTAT"

            Emneknagg.Regelknagg.GJENOPPTAK -> "GJENOPPTAK"

            Emneknagg.Regelknagg.AVSLAG_MINSTEINNTEKT,
            Emneknagg.Regelknagg.AVSLAG_ARBEIDSINNTEKT,
            Emneknagg.Regelknagg.AVSLAG_ARBEIDSTID,
            Emneknagg.Regelknagg.AVSLAG_ALDER,
            Emneknagg.Regelknagg.AVSLAG_ANDRE_YTELSER,
            Emneknagg.Regelknagg.AVSLAG_STREIK,
            Emneknagg.Regelknagg.AVSLAG_OPPHOLD_UTLAND,
            Emneknagg.Regelknagg.AVSLAG_REELL_ARBEIDSSØKER,
            Emneknagg.Regelknagg.AVSLAG_IKKE_REGISTRERT,
            Emneknagg.Regelknagg.AVSLAG_UTESTENGT,
            Emneknagg.Regelknagg.AVSLAG_UTDANNING,
            Emneknagg.Regelknagg.AVSLAG_MEDLEMSKAP,
            -> "AVSLAGSGRUNN"

            Emneknagg.Regelknagg.RETTIGHET_ORDINÆR,
            Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT,
            Emneknagg.Regelknagg.RETTIGHET_PERMITTERT,
            Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK,
            Emneknagg.Regelknagg.RETTIGHET_KONKURS,
            -> "RETTIGHET"
        }
    }

    Emneknagg.PåVent.entries.find { it.visningsnavn == visningsNavn }?.let {
        return "PÅ_VENT"
    }

    Emneknagg.AvbrytBehandling.entries.find { it.visningsnavn == visningsNavn }?.let {
        return "AVBRUTT_GRUNN"
    }

    return "UDEFINERT"
}
