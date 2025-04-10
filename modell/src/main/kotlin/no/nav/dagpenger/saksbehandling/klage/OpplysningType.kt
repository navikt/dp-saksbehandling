package no.nav.dagpenger.saksbehandling.klage

enum class OpplysningType(
    val navn: String,
    val datatype: Datatype,
) {
    // Klagen gjelder
    KLAGEN_GJELDER(
        navn = "Hva klagen gjelder",
        datatype = Datatype.FLERVALG,
    ),

    // Formkrav
    ER_KLAGEN_SKRIFTLIG(
        navn = "Er klagen skriftlig",
        datatype = Datatype.BOOLSK,
    ),

    ER_KLAGEN_UNDERSKREVET(
        navn = "Er klagen underskrevet",
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
    ),

    VURDERNIG_AV_KLAGEN(
        navn = "Vurdering av klagen",
        datatype = Datatype.TEKST,
    ),

    // Info til klageinstans del 1
    HVEM_KLAGER(
        navn = "Hvem er klager i saken?",
        datatype = Datatype.TEKST,
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
    ),
    FULLMEKTIG_ADRESSE_3(
        navn = "Adresselinje 3",
        datatype = Datatype.TEKST,
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

    // Info til klageinstans del 2
    HJEMLER(
        navn = "Hvilke hjemler gjelder klagen?",
        datatype = Datatype.TEKST,
    ),
    INTERN_MELDING(
        navn = "Intern melding",
        datatype = Datatype.TEKST,
    ),
}

enum class Datatype {
    TEKST,
    DATO,
    BOOLSK,
    FLERVALG,
}
