package no.nav.dagpenger.saksbehandling.klage

enum class OpplysningType(
    val navn: String,
    val datatype: Datatype,
    val påkrevd: Boolean = true,
    val valgmuligheter: List<String> = emptyList(),
) {
    // Klagen gjelder
    KLAGEN_GJELDER(
        navn = "Hva klagen gjelder",
        datatype = Datatype.FLERVALG,
        påkrevd = false,
        valgmuligheter = listOf("Avslag på søknad", "For lite utbetalt", "Vedtak om tilbakebetaling", "Annet"),
    ),
    KLAGEN_GJELDER_VEDTAK(
        navn = "Vedtak klagen gjelder",
        datatype = Datatype.TEKST,
    ),
    KLAGEN_GJELDER_VEDTAKSDATO(
        navn = "Vedtaksdato",
        datatype = Datatype.DATO,
    ),

    // Formkrav
    ER_KLAGEN_SKRIFTLIG(
        navn = "Er klagen skriftlig?",
        datatype = Datatype.BOOLSK,
    ),
    ER_KLAGEN_UNDERSKREVET(
        navn = "Er klagen underskrevet?",
        datatype = Datatype.BOOLSK,
    ),
    KLAGEN_NEVNER_ENDRING(
        navn = "Nevner klagen den endring som kreves?",
        datatype = Datatype.BOOLSK,
    ),
    RETTSLIG_KLAGEINTERESSE(
        navn = "Har klager rettslig klageinteresse?",
        datatype = Datatype.BOOLSK,
    ),

    // Klagefrist
    KLAGE_MOTTATT(
        navn = "Klage mottatt",
        datatype = Datatype.DATO,
    ),
    KLAGEFRIST(
        navn = "Frist for å klage",
        datatype = Datatype.DATO,
    ),
    KLAGEFRIST_OPPFYLT(
        navn = "Har klager klaget innen fristen?",
        datatype = Datatype.BOOLSK,
    ),

    // Oversittet frist
    OPPREISNING_OVERSITTET_FRIST(
        navn = "Gis klager oppreisning for oversittet frist?",
        datatype = Datatype.BOOLSK,
    ),

    OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE(
        navn = "Begrunnelse",
        datatype = Datatype.TEKST,
    ),

    // Utfall
    UTFALL(
        navn = "Utfall",
        datatype = Datatype.TEKST,
        valgmuligheter = UtfallType.entries.map { it.name },
    ),

    VURDERIG_AV_KLAGEN(
        navn = "Vurdering av klagen",
        datatype = Datatype.TEKST,
    ),

    // Info til klageinstans del 1
    HVEM_KLAGER(
        navn = "Hvem er klager i saken?",
        datatype = Datatype.TEKST,
        valgmuligheter = HvemKlagerType.entries.map { it.name },
    ),

    // Info til klageinstans del 2
    HJEMLER(
        navn = "Hvilke hjemler gjelder klagen?",
        datatype = Datatype.FLERVALG,
        valgmuligheter = listOf("§ 4-1", "§ 4-2", "§ 4-3", "§ 4-4", "§ 4-5"),
    ),
    INTERN_MELDING(
        navn = "Intern melding",
        datatype = Datatype.TEKST,
        påkrevd = false,
    ),

    // Info til klageinstans om fullmektig
    FULLMEKTIG_NAVN(
        navn = "Navn",
        datatype = Datatype.TEKST,
    ),
    FULLMEKTIG_ADRESSE_1(
        navn = "Adresselinje 1",
        datatype = Datatype.TEKST,
    ),
    FULLMEKTIG_ADRESSE_2(
        navn = "Adresselinje 2",
        datatype = Datatype.TEKST,
        påkrevd = false,
    ),
    FULLMEKTIG_ADRESSE_3(
        navn = "Adresselinje 3",
        datatype = Datatype.TEKST,
        påkrevd = false,
    ),
    FULLMEKTIG_POSTNR(
        navn = "Postnummer",
        datatype = Datatype.TEKST,
    ),
    FULLMEKTIG_POSTSTED(
        navn = "Poststed",
        datatype = Datatype.TEKST,
    ),
    FULLMEKTIG_LAND(
        navn = "Land",
        datatype = Datatype.TEKST,
    ),
}

enum class Datatype {
    TEKST,
    DATO,
    BOOLSK,
    FLERVALG,
}

enum class UtfallType {
    OPPRETTHOLDELSE,
    MEDHOLD,
    DELVIS_MEDHOLD,
    AVVIST,
}

enum class HvemKlagerType {
    BRUKER,
    FULLMEKTIG,
}

enum class KlagenGjelderType {
    AVSLAG_PÅ_SØKNAD,
    DAGPENGENES_STØRRELSE,
    ANNET,
}

enum class Hjemler {
    FTRL_4_2,
    FTRL_4_3_1,
    FTRL_4_3_1_PERMLL,
    FTRL_4_3_2,
    FTRL_4_4,
    FTRL_4_5_REGISTRERING,
    FTRL_4_5_REELL_ARBEIDSSOEKER,
    FTRL_4_6_UTDANNING,
    FTRL_4_6_ETABLERING,
    FTRL_4_7_PERMITTERINGSAARSAK,
    FTRL_4_7_PERMITTERINGENS_LENGDE,
    FTRL_4_8,
    FTRL_4_9,
    FTRL_4_10,
    FTRL_4_11,
    FTRL_4_12,
    FTRL_4_13,
    FTRL_4_14,
    FTRL_4_15,
    FTRL_4_16,
    FTRL_4_18,
    FTRL_4_19,
    FTRL_4_20,
    FTRL_4_22,
    FTRL_4_23,
    FTRL_4_24,
    FTRL_4_25,
    FTRL_4_26,
    FTRL_4_27,
    FTRL_4_28,
}
