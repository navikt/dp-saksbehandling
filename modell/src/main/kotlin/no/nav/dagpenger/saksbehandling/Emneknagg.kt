package no.nav.dagpenger.saksbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

interface KategorisertEmneknagg {
    val visningsnavn: String
    val kategori: EmneknaggKategori
}

object Emneknagg {
    data class Ettersending(
        private val mottatt: LocalDate = LocalDate.now(),
    ) : KategorisertEmneknagg {
        val fastTekst = "Ettersending"
        override val visningsnavn = "$fastTekst($mottatt)"
        override val kategori = EmneknaggKategori.ETTERSENDING
    }

    enum class Regelknagg(
        override val visningsnavn: String,
        override val kategori: EmneknaggKategori,
    ) : KategorisertEmneknagg {
        AVSLAG("Avslag", EmneknaggKategori.SØKNADSRESULTAT),
        INNVILGELSE("Innvilgelse", EmneknaggKategori.SØKNADSRESULTAT),
        GJENOPPTAK("Gjenopptak", EmneknaggKategori.GJENOPPTAK),
        AVSLAG_MINSTEINNTEKT("Minsteinntekt", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_ARBEIDSINNTEKT("Arbeidsinntekt", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_ARBEIDSTID("Arbeidstid", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_ALDER("Alder", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_ANDRE_YTELSER("Andre ytelser", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_STREIK("Streik", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_OPPHOLD_UTLAND("Opphold utland", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_REELL_ARBEIDSSØKER("Reell arbeidssøker", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_IKKE_REGISTRERT("Ikke registrert", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_UTESTENGT("Utestengt", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_UTDANNING("Utdanning", EmneknaggKategori.AVSLAGSGRUNN),
        AVSLAG_MEDLEMSKAP("Medlemskap", EmneknaggKategori.AVSLAGSGRUNN),
        RETTIGHET_ORDINÆR("Ordinær", EmneknaggKategori.RETTIGHET),
        RETTIGHET_VERNEPLIKT("Verneplikt", EmneknaggKategori.RETTIGHET),
        RETTIGHET_PERMITTERT("Permittert", EmneknaggKategori.RETTIGHET),
        RETTIGHET_PERMITTERT_FISK("Permittert fisk", EmneknaggKategori.RETTIGHET),
        RETTIGHET_KONKURS("Konkurs", EmneknaggKategori.RETTIGHET),
        RETTIGHET_FERIETILLEGG("Ferietillegg", EmneknaggKategori.RETTIGHET),
        BEHANDLET_HENDELSE_TYPE_ARBEIDSSØKERPERIODE("Arbeidssøkerperiode", EmneknaggKategori.BEHANDLET_HENDELSE_TYPE),
        BEHANDLET_HENDELSE_TYPE_SAMORDNING("Samordning", EmneknaggKategori.BEHANDLET_HENDELSE_TYPE),
        EKSPORT("Eksport", EmneknaggKategori.UDEFINERT),
    }

    enum class PåVent(
        override val visningsnavn: String,
    ) : KategorisertEmneknagg {
        AVVENT_SVAR("Avvent svar"),
        AVVENT_DOKUMENTASJON("Avvent dokumentasjon"),
        AVVENT_MELDEKORT("Avvent meldekort"),
        AVVENT_PERMITTERINGSÅRSAK("Avvent permitteringsårsak"),
        AVVENT_RAPPORTERINGSFRIST("Avvent rapporteringsfrist"),
        AVVENT_SVAR_PÅ_FORESPØRSEL("Sendt forespørsel"),
        AVVENT_SØKT_FOR_TIDLIG("Søkt for tidlig"),
        MANGLENDE_FUNKSJONALITET("Manglende funksjonalitet"),
        AVVENT_ANNET("Utsatt annen årsak"),
        TIDLIGERE_UTSATT("Tidligere utsatt"),
        ;

        override val kategori = EmneknaggKategori.PÅ_VENT
    }

    enum class AvbrytBehandling(
        override val visningsnavn: String,
    ) : KategorisertEmneknagg {
        AVBRUTT_BEHANDLES_I_ARENA("Behandles i Arena"),
        AVBRUTT_FLERE_SØKNADER("Flere søknader"),
        AVBRUTT_TRUKKET_SØKNAD("Trukket søknad"),
        AVBRUTT_INGEN_BEHANDLING("Ingen behandling"),
        AVBRUTT_ANNET("Annen avbruddsårsak"),
        ;

        override val kategori = EmneknaggKategori.AVBRUTT_GRUNN
    }

    enum class AvbrytKlage(
        override val visningsnavn: String,
    ) : KategorisertEmneknagg {
        AVBRUTT_FLERE_KLAGER("Flere klager"),
        AVBRUTT_TRUKKET_KLAGE("Trukket klage"),
        AVBRUTT_ANNET("Annen avbruddsårsak klage"),
        ;

        override val kategori = EmneknaggKategori.AVBRUTT_GRUNN
    }

    enum class Oppfølging(
        override val visningsnavn: String,
    ) : KategorisertEmneknagg {
        AVVENTER_NY_INFORMASJON("Avventer ny informasjon"),
        KOPI_AV_VEDTAK_TIL_FULLMEKTIG("Kopi av vedtak til fullmektig"),
        OPPFØLGING_AV_MELDEKORT("Oppfølging av meldekort"),
        OPPFØLGING_AV_SAMORDNING("Oppfølging av samordning"),
        OPPFØLGING_AV_VEDTAK("Oppfølging av vedtak"),
        VEDTAK_FRA_KLAGEINSTANSEN("Vedtak fra klageinstansen"),
        VURDERE_FEILUTBETALING("Vurdere feilutbetaling"),
        OMPOSTERING("Ompostering"),
        ANNEN_ÅRSAK("Annen årsak"),
        ;

        override val kategori = EmneknaggKategori.OPPFØLGING_ÅRSAK
    }

    enum class Kontroll(
        override val visningsnavn: String,
    ) : KategorisertEmneknagg {
        RETUR_FRA_KONTROLL("Retur fra kontroll"),
        TIDLIGERE_KONTROLLERT("Tidligere kontrollert"),
        BEHANDLING_OPPDATERT("Behandling oppdatert"),
        ;

        override val kategori = EmneknaggKategori.UDEFINERT
    }

    enum class Søknadsavklaring(
        override val visningsnavn: String,
    ) : KategorisertEmneknagg {
        EØS_INNTEKT("EØS-inntekt"),
        BOSATT_UTLAND("Bosatt utland"),
        GRENSEARBEIDER("Grensearbeider"),
        MULIG_SANKSJON("Mulig sanksjon"),
        BARN_OVER_16("Barn over 16"),
        PLANLEGGER_UTDANNING("Planlegger utdanning"),
        EØS_PENGESTØTTE("EØS-pengestøtte"),
        D_NUMMER("D-nummer"),
        ;

        override val kategori = EmneknaggKategori.UDEFINERT
    }

    enum class Klage(
        override val visningsnavn: String,
    ) : KategorisertEmneknagg {
        KLAGE_AVVIST("Avvist"),
        KLAGE_MEDHOLD("Medhold"),
        KLAGE_DELVIS_MEDHOLD("Delvis medhold"),
        KLAGE_OVERSENDT_KLAGEINSTANS("Oversendt klageinstans"),
        KLAGE_UTFALL_KLAGEINSTANS_TRUKKET("Trukket klageinstans"),
        KLAGE_UTFALL_KLAGEINSTANS_RETUR("Retur fra klageinstans"),
        KLAGE_UTFALL_KLAGEINSTANS_OPPHEVET("Opphevet av klageinstans"),
        KLAGE_UTFALL_KLAGEINSTANS_MEDHOLD("Medhold hos klageinstans"),
        KLAGE_UTFALL_KLAGEINSTANS_DELVIS_MEDHOLD("Delvis medhold hos klageinstans"),
        KLAGE_UTFALL_KLAGEINSTANS_STADFESTELSE("Stadfestelse hos klageinstans"),
        KLAGE_UTFALL_KLAGEINSTANS_UGUNST("Ugunst hos klageinstans"),
        KLAGE_UTFALL_KLAGEINSTANS_AVVIST("Avvist hos klageinstans"),
        KLAGE_UTFALL_KLAGEINSTANS_HENLAGT("Henlagt av klageinstans"),
        ;

        override val kategori = EmneknaggKategori.UDEFINERT

        companion object {
            fun fromUtfallType(utfall: UtfallType): Klage =
                when (utfall) {
                    UtfallType.OPPRETTHOLDELSE -> KLAGE_OVERSENDT_KLAGEINSTANS
                    UtfallType.MEDHOLD -> KLAGE_MEDHOLD
                    UtfallType.DELVIS_MEDHOLD -> KLAGE_DELVIS_MEDHOLD
                    UtfallType.AVVIST -> KLAGE_AVVIST
                }
        }
    }

    val alleKodedefinerte: List<KategorisertEmneknagg> =
        Regelknagg.entries + PåVent.entries + AvbrytBehandling.entries +
            Oppfølging.entries + Kontroll.entries + Søknadsavklaring.entries

    fun utfallKlageinstansTilEmneknagg(utfallString: String): Klage =
        when (utfallString) {
            "TRUKKET" -> Klage.KLAGE_UTFALL_KLAGEINSTANS_TRUKKET
            "RETUR" -> Klage.KLAGE_UTFALL_KLAGEINSTANS_RETUR
            "OPPHEVET" -> Klage.KLAGE_UTFALL_KLAGEINSTANS_OPPHEVET
            "MEDHOLD" -> Klage.KLAGE_UTFALL_KLAGEINSTANS_MEDHOLD
            "DELVIS_MEDHOLD" -> Klage.KLAGE_UTFALL_KLAGEINSTANS_DELVIS_MEDHOLD
            "STADFESTELSE" -> Klage.KLAGE_UTFALL_KLAGEINSTANS_STADFESTELSE
            "UGUNST" -> Klage.KLAGE_UTFALL_KLAGEINSTANS_UGUNST
            "AVVIST" -> Klage.KLAGE_UTFALL_KLAGEINSTANS_AVVIST
            "HENLAGT" -> Klage.KLAGE_UTFALL_KLAGEINSTANS_HENLAGT
            else -> throw IllegalArgumentException("Ukjent utfall fra klageinstans: $utfallString")
        }
}

fun hentEmneknaggKategori(visningsnavn: String): EmneknaggKategori =
    if (visningsnavn.startsWith("Ettersending")) {
        EmneknaggKategori.ETTERSENDING
    } else {
        Emneknagg.alleKodedefinerte.find { it.visningsnavn == visningsnavn }?.kategori
            ?: EmneknaggKategori.UDEFINERT
    }

enum class EmneknaggKategori {
    RETTIGHET,
    GJENOPPTAK,
    SØKNADSRESULTAT,
    AVSLAGSGRUNN,
    AVBRUTT_GRUNN,
    PÅ_VENT,
    ETTERSENDING,
    OPPFØLGING_ÅRSAK,
    BEHANDLET_HENDELSE_TYPE,
    UDEFINERT,
}
