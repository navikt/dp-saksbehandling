package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.api.models.EmneknaggKategoriDTO
import org.junit.jupiter.api.Test

class EmneknaggKategoriseringTest {
    @Test
    fun `Regelknagger skal mappes til riktig kategori`() {
        val emneknagger =
            setOf(
                Emneknagg.Regelknagg.AVSLAG.visningsnavn,
                Emneknagg.Regelknagg.INNVILGELSE.visningsnavn,
                Emneknagg.Regelknagg.GJENOPPTAK.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_MINSTEINNTEKT.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_ORDINÆR.visningsnavn,
            )

        val result = emneknagger.tilOppgaveEmneknaggerDTOListe()

        result.single { it.visningsnavn == "Avslag" }.kategori shouldBe EmneknaggKategoriDTO.SOKNADSRESULTAT
        result.single { it.visningsnavn == "Innvilgelse" }.kategori shouldBe EmneknaggKategoriDTO.SOKNADSRESULTAT
        result.single { it.visningsnavn == "Gjenopptak" }.kategori shouldBe EmneknaggKategoriDTO.GJENOPPTAK
        result.single { it.visningsnavn == "Minsteinntekt" }.kategori shouldBe EmneknaggKategoriDTO.AVSLAGSGRUNN
        result.single { it.visningsnavn == "Ordinær" }.kategori shouldBe EmneknaggKategoriDTO.RETTIGHET
    }

    @Test
    fun `Alle avslagsgrunner skal ha kategori AVSLAGSGRUNN`() {
        val avslagsgrunner =
            setOf(
                Emneknagg.Regelknagg.AVSLAG_MINSTEINNTEKT.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_ARBEIDSINNTEKT.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_ARBEIDSTID.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_ALDER.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_ANDRE_YTELSER.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_STREIK.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_OPPHOLD_UTLAND.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_REELL_ARBEIDSSØKER.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_IKKE_REGISTRERT.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_UTESTENGT.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_UTDANNING.visningsnavn,
                Emneknagg.Regelknagg.AVSLAG_MEDLEMSKAP.visningsnavn,
            )

        val result = avslagsgrunner.tilOppgaveEmneknaggerDTOListe()

        result.forEach { emneknagg ->
            emneknagg.kategori shouldBe EmneknaggKategoriDTO.AVSLAGSGRUNN
        }
    }

    @Test
    fun `Alle rettighetsknagger skal ha kategori RETTIGHET`() {
        val rettigheter =
            setOf(
                Emneknagg.Regelknagg.RETTIGHET_ORDINÆR.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_PERMITTERT.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK.visningsnavn,
                Emneknagg.Regelknagg.RETTIGHET_KONKURS.visningsnavn,
            )

        val result = rettigheter.tilOppgaveEmneknaggerDTOListe()

        result.forEach { emneknagg ->
            emneknagg.kategori shouldBe EmneknaggKategoriDTO.RETTIGHET
        }
    }

    @Test
    fun `PåVent knagger skal ha kategori PÅ_VENT`() {
        val påVentKnagger =
            setOf(
                Emneknagg.PåVent.AVVENT_SVAR.visningsnavn,
                Emneknagg.PåVent.AVVENT_DOKUMENTASJON.visningsnavn,
                Emneknagg.PåVent.AVVENT_MELDEKORT.visningsnavn,
            )

        val result = påVentKnagger.tilOppgaveEmneknaggerDTOListe()

        result.forEach { emneknagg ->
            emneknagg.kategori shouldBe EmneknaggKategoriDTO.PAA_VENT
        }
    }

    @Test
    fun `AvbrytBehandling knagger skal ha kategori AVBRUTT_GRUNN`() {
        val avbrytKnagger =
            setOf(
                Emneknagg.AvbrytBehandling.AVBRUTT_BEHANDLES_I_ARENA.visningsnavn,
                Emneknagg.AvbrytBehandling.AVBRUTT_FLERE_SØKNADER.visningsnavn,
                Emneknagg.AvbrytBehandling.AVBRUTT_TRUKKET_SØKNAD.visningsnavn,
                Emneknagg.AvbrytBehandling.AVBRUTT_ANNET.visningsnavn,
            )

        val result = avbrytKnagger.tilOppgaveEmneknaggerDTOListe()

        result.forEach { emneknagg ->
            emneknagg.kategori shouldBe EmneknaggKategoriDTO.AVBRUTT_GRUNN
        }
    }

    @Test
    fun `Ettersending skal ha kategori ETTERSENDING`() {
        val ettersendingKnagger =
            setOf(
                "Ettersending(2024-01-15)",
                "Ettersending(2024-02-20)",
            )

        val result = ettersendingKnagger.tilOppgaveEmneknaggerDTOListe()

        result.forEach { emneknagg ->
            emneknagg.kategori shouldBe EmneknaggKategoriDTO.ETTERSENDING
        }
    }

    @Test
    fun `Ukjent emneknagg skal ha kategori UDEFINERT`() {
        val ukjenteKnagger =
            setOf(
                "Noe helt nytt",
                "Ukjent kategori",
            )

        val result = ukjenteKnagger.tilOppgaveEmneknaggerDTOListe()

        result.forEach { emneknagg ->
            emneknagg.kategori shouldBe EmneknaggKategoriDTO.UDEFINERT
        }
    }

    @Test
    fun `Mikset liste av emneknagger skal kategoriseres riktig`() {
        val miksedeKnagger =
            setOf(
                Emneknagg.Regelknagg.AVSLAG.visningsnavn,
                Emneknagg.PåVent.AVVENT_SVAR.visningsnavn,
                "Ettersending(2024-01-15)",
                Emneknagg.AvbrytBehandling.AVBRUTT_ANNET.visningsnavn,
                "Ukjent knagg",
            )

        val result = miksedeKnagger.tilOppgaveEmneknaggerDTOListe()

        result.size shouldBe 5
        result.single { it.visningsnavn == "Avslag" }.kategori shouldBe EmneknaggKategoriDTO.SOKNADSRESULTAT
        result.single { it.visningsnavn == "Avvent svar" }.kategori shouldBe EmneknaggKategoriDTO.PAA_VENT
        result.single { it.visningsnavn.startsWith("Ettersending") }.kategori shouldBe EmneknaggKategoriDTO.ETTERSENDING
        result.single { it.visningsnavn == "Annen avbruddsårsak" }.kategori shouldBe EmneknaggKategoriDTO.AVBRUTT_GRUNN
        result.single { it.visningsnavn == "Ukjent knagg" }.kategori shouldBe EmneknaggKategoriDTO.UDEFINERT
    }
}
