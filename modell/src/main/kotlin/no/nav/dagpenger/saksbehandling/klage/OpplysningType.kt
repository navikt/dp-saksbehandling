package no.nav.dagpenger.saksbehandling.klage

enum class OpplysningType(
    val navn: String,
    val datatype: Datatype,
    val påkrevd: Boolean = true,
    val valgmuligheter: List<String> = emptyList(),
    val regler: Set<Regel> = emptySet(),
) {
    // Klagen gjelder
    KLAGEN_GJELDER(
        navn = "Hva klagen gjelder",
        datatype = Datatype.FLERVALG,
        påkrevd = false,
        valgmuligheter = KlagenGjelderType.entries.map { it.tekst },
    ),
    KLAGEN_GJELDER_VEDTAK(
        navn = "Vedtak klagen gjelder",
        datatype = Datatype.TEKST,
    ),
    KLAGEN_GJELDER_VEDTAKSDATO(
        navn = "Vedtaksdato",
        datatype = Datatype.DATO,
        regler = setOf(IkkeIFremtiden),
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
        valgmuligheter = UtfallType.entries.map { it.tekst },
    ),

    VURDERING_AV_KLAGEN(
        navn = "Vurdering av klagen",
        datatype = Datatype.TEKST,
    ),

    // Info til klageinstans del 1
    HVEM_KLAGER(
        navn = "Hvem er klager i saken?",
        datatype = Datatype.TEKST,
        valgmuligheter = HvemKlagerType.entries.map { it.tekst },
    ),

    // Info til klageinstans del 2
    HJEMLER(
        navn = "Hvilke hjemler gjelder klagen?",
        datatype = Datatype.FLERVALG,
        valgmuligheter = Hjemler.entries.map { it.tittel },
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
        valgmuligheter = Land.entries.map { it.land },
    ),
}

enum class Datatype {
    TEKST,
    DATO,
    BOOLSK,
    FLERVALG,
}

enum class UtfallType(val tekst: String) {
    OPPRETTHOLDELSE("Opprettholdelse"),
    MEDHOLD("Medhold"),
    DELVIS_MEDHOLD("Delvis medhold"),
    AVVIST("Avvist"),
    ;

    companion object {
        fun Verdi.toUtfallType(): UtfallType? {
            return when (this) {
                is Verdi.TekstVerdi -> {
                    UtfallType.entries.singleOrNull { it.tekst == this.value } ?: UtfallType.valueOf(this.value)
                }
                is Verdi.TomVerdi -> null
                else -> throw IllegalArgumentException("Kan ikke konvertere verdi av type ${this::class.simpleName} til UtfallType")
            }
        }
    }
}

enum class HvemKlagerType(val tekst: String) {
    BRUKER("Bruker"),
    FULLMEKTIG("Fullmektig"),
    ;

    companion object {
        fun Verdi.toHvemKlagerType(): HvemKlagerType? {
            return when (this) {
                is Verdi.TekstVerdi -> {
                    HvemKlagerType.entries.singleOrNull { it.tekst == this.value } ?: HvemKlagerType.valueOf(this.value)
                }
                is Verdi.TomVerdi -> null
                else -> throw IllegalArgumentException("Kan ikke konvertere verdi av type ${this::class.simpleName} til HvemKlagerType")
            }
        }
    }
}

enum class KlagenGjelderType(val tekst: String) {
    AVSLAG_PÅ_SØKNAD("Avslag på søknad"),
    FOR_LITE_UTBETALT("For lite utbetalt"),
    VEDTAK_OM_TILBAKEBETALING("Vedtak om tilbakebetaling"),
    ANNET("Annet"),
}

enum class Hjemler(val tittel: String) {
    FTRL_4_2("§ 4-2 Krav til opphold i Norge"),
    FTRL_4_3_2("§ 4-3 Krav til tap av arbeidstid"),
    FTRL_4_3_1("§ 4-3 Krav til tap av arbeidsinntekt"),
    FTRL_4_4("§ 4-4 Krav til minsteinntekt"),
    FTRL_4_5_REELL_ARBEIDSSOEKER("§ 4-5 Krav til å være reell arbeidssøker"),
    FTRL_4_5_REGISTRERING("§ 4-5 Krav til å være registrert som arbeidssøker for å være reell arbeidssøker"),
    FTRL_4_6_UTDANNING("§ 4-6 Dagpenger under utdanning/opplæring"),
    FTRL_4_6_ETABLERING("§ 4-6 Dagpenger under etablering av egen virksomhet"),
    FTRL_4_8("§ 4-8 Krav til å opprettholde melde – og møteplikt"),
    FTRL_4_9("§ 4-9 Egenandel"),
    FTRL_4_10("§ 4-10 Sanksjonsperiode ved selvforskyldt arbeidsløshet"),
    FTRL_4_11("§ 4-11 Dagpengegrunnlag"),
    FTRL_4_12("§ 4-12 Dagpenger størrelse"),
    FTRL_4_13("§ 4-13 Graderte dagpenger"),
    FTRL_4_14("§ 4-14 Ferietillegg"),
    FTRL_4_15("§ 4-15 Antall stønadsuker"),
    FTRL_4_16("§ 4-16 Gjenopptak av stønadsperiode"),
    FTRL_4_18("§ 4-18 Dagpenger til fiskere og fangstmenn"),
    FTRL_4_19("§ 4-19 Dagpenger etter avtjent verneplikt"),
    FTRL_4_20("§ 4-20 Tidsbegrenset bortfall av dagpenger"),

    FTRL_4_22("§ 4-22 Bortfall ved streik og lock-out"),
    FTRL_4_23("§ 4-23 Bortfall på grunn av alder"),
    FTRL_4_24("§ 4-24 Mottar fulle ytelser etter folketrygdloven eller avtalefestet pensjon"),
    FTRL_4_25("§ 4-25 Samordning med reduserte ytelser fra folketrygdloven eller redusert avtalefestet pensjon"),
    FTRL_4_26("§ 4-26 Samordning med ytelser utenfor folketrygden"),
    FTRL_4_27("§ 4-27 Fellesbestemmelser ved bortfall/samordning"),
    FTRL_4_28("§ 4-28 Utestengning"),
    FTRL_21_3("§ 21-3 Medlemmets opplysningsplikt"),
    FTRL_21_7("§ 21-7 Uriktige opplysninger"),
    FTRL_22_13("§ 22-13 Framsetting av krav - og virkningstidspunkt"),
    FTRL_22_14("§ 22-14 Foreldelse"),
    FTRL_22_15("§ 22-15 Tilbakekreving etter feilaktig utbetaling"),
    FTRL_22_17("§ 22-17 Renter ved etterbetaling av ytelser"),
    FVL_35("§ 35 Omgjøring av vedtak uten klage"),
    EOES_883_2004("Trygdeforordning 883/2004"),
    EOES_883_2004_11("Trygdeforordning 883/2004 artikkel 11- Generelle regler lovvalg"),
    EOES_883_2004_12("Trygdeforordning 883/2004 artikkel 12- Utsendt arbeidstaker"),
    EOES_883_2004_13("Trygdeforordning 883/2004 artikkel 13- Aktivitet i to eller flere medlemstater"),
    EOES_883_2004_61("Trygdeforordning 883/2004 artikkel 61 Særregler om sammenlegging av trygdetid"),
    EOES_883_2004_62("Trygdeforordning 883/2004 artikkel 62 beregning av ytelser"),
    EOES_883_2004_64("Trygdeforordning 883/2004, artikkel 64 – Arbeidsløse som reiser til en annen medlemsstat (eksport)"),

    /*FTRL_4_7_PERMITTERINGSAARSAK,
    FTRL_4_7_PERMITTERINGENS_LENGDE,
     */
}

// TODO: Vurder om vi burde importere et bibliotek/kodeverk som dekker behovet vårt her.
// det blir for mye styr å opprettholde dette når det er mange nye land :)
enum class Land(val land: String) {
    UKJENT("Uoppgitt/Ukjent"),
    AD("Andorra"),
    AE("De forente arabiske emirater"),
    AF("Afghanistan"),
    AG("Antigua og Barbuda"),
    AI("Anguilla"),
    AL("Albania"),
    AM("Armenia"),
    AO("Angola"),
    AQ("Antarktis"),
    AR("Argentina"),
    AS("Amerikansk Samoa"),
    AT("Østerrike"),
    AU("Australia"),
    AW("Aruba"),
    AX("Åland"),
    AZ("Aserbajdsjan"),
    BA("Bosnia-Hercegovina"),
    BB("Barbados"),
    BD("Bangladesh"),
    BE("Belgia"),
    BF("Burkina Faso"),
    BG("Bulgaria"),
    BH("Bahrain"),
    BI("Burundi"),
    BJ("Benin"),
    BL("Saint-Barthélemy"),
    BM("Bermuda"),
    BN("Brunei"),
    BO("Bolivia"),
    BQ("Bonaire, Sint Eustatius og Saba"),
    BR("Brasil"),
    BS("Bahamas"),
    BT("Bhutan"),
    BV("Bouvetøya"),
    BW("Botswana"),
    BY("Belarus"),
    BZ("Belize"),
    CA("Canada"),
    CC("Kokosøyene"),
    CD("Den demokratiske republikken Kongo"),
    CF("Den sentralafrikanske republikk"),
    CG("Republikken Kongo"),
    CH("Sveits"),
    CI("Elfenbenskysten"),
    CK("Cookøyene"),
    CL("Chile"),
    CM("Kamerun"),
    CN("Kina"),
    CO("Colombia"),
    CR("Costa Rica"),

    // CS Serbia og Montenegro er ikke i bruk lengre
    // men Folkeregisteret har personer med gjeldende statsborkerskap eller fødeland her.
    // Dermed er koden i felles kodeverk.
    CS("Serbia og Montenegro"),

    CU("Cuba"),
    CV("Kapp Verde"),
    CW("Curaçao"),
    CX("Christmasøya"),
    CY("Kypros"),
    CZ("Tsjekkia"),
    DE("Tyskland"),
    DJ("Djibouti"),
    DK("Danmark"),
    DM("Dominica"),
    DO("Den dominikanske republikk"),
    DZ("Algerie"),
    EC("Ecuador"),
    EE("Estland"),
    EG("Egypt"),
    EH("Vest-Sahara"),
    ER("Eritrea"),
    ES("Spania"),
    ET("Etiopia"),
    FI("Finland"),
    FJ("Fiji"),
    FK("Falklandsøyene"),
    FM("Mikronesiaføderasjonen"),
    FO("Færøyene"),
    FR("Frankrike"),
    GA("Gabon"),
    GB("Storbritannia"),
    GD("Grenada"),
    GE("Georgia"),
    GF("Fransk Guyana"),
    GG("Guernsey"),
    GH("Ghana"),
    GI("Gibraltar"),
    GL("Grønland"),
    GM("Gambia"),
    GN("Guinea"),
    GP("Guadeloupe"),
    GQ("Ekvatorial-Guinea"),
    GR("Hellas"),
    GS("Sør-Georgia og Sør-Sandwichøyene"),
    GT("Guatemala"),
    GU("Guam"),
    GW("Guinea-Bissau"),
    GY("Guyana"),
    HK("Hongkong"),
    HM("Heard- og McDonaldøyene"),
    HN("Honduras"),
    HR("Kroatia"),
    HT("Haiti"),
    HU("Ungarn"),
    ID("Indonesia"),
    IE("Irland"),
    IL("Israel"),
    IM("Man"),
    IN("India"),
    IO("Det britiske territoriet i Indiahavet"),
    IQ("Irak"),
    IR("Iran"),
    IS("Island"),
    IT("Italia"),
    JE("Jersey"),
    JM("Jamaica"),
    JO("Jordan"),
    JP("Japan"),
    KE("Kenya"),
    KG("Kirgisistan"),
    KH("Kambodsja"),
    KI("Kiribati"),
    KM("Komorene"),
    KN("Saint Kitts og Nevis"),
    KP("Nord-Korea"),
    KR("Sør-Korea"),
    KW("Kuwait"),
    KY("Caymanøyene"),
    KZ("Kasakhstan"),
    LA("Laos"),
    LB("Libanon"),
    LC("Saint Lucia"),
    LI("Liechtenstein"),
    LK("Sri Lanka"),
    LR("Liberia"),
    LS("Lesotho"),
    LT("Litauen"),
    LU("Luxembourg"),
    LV("Latvia"),
    LY("Libya"),
    MA("Marokko"),
    MC("Monaco"),
    MD("Moldova"),
    ME("Montenegro"),
    MF("Saint-Martin"),
    MG("Madagaskar"),
    MH("Marshalløyene"),
    MK("Nord-Makedonia"),
    ML("Mali"),
    MM("Myanmar"),
    MN("Mongolia"),
    MO("Macao"),
    MP("Nord-Marianene"),
    MQ("Martinique"),
    MR("Mauritania"),
    MS("Montserrat"),
    MT("Malta"),
    MU("Mauritius"),
    MV("Maldivene"),
    MW("Malawi"),
    MX("Mexico"),
    MY("Malaysia"),
    MZ("Mosambik"),
    NA("Namibia"),
    NC("Ny-Caledonia"),
    NE("Niger"),
    NF("Norfolkøya"),
    NG("Nigeria"),
    NI("Nicaragua"),
    NL("Nederland"),
    NO("Norge"),
    NP("Nepal"),
    NR("Nauru"),
    NU("Niue"),
    NZ("New Zealand"),
    OM("Oman"),
    PA("Panama"),
    PE("Peru"),
    PF("Fransk Polynesia"),
    PG("Papua Ny-Guinea"),
    PH("Filippinene"),
    PK("Pakistan"),
    PL("Polen"),
    PM("Saint-Pierre og Miquelon"),
    PN("Pitcairnøyene"),
    PR("Puerto Rico"),
    PS("Palestina"),
    PT("Portugal"),
    PW("Palau"),
    PY("Paraguay"),
    QA("Qatar"),
    RE("Réunion"),
    RO("Romania"),
    RS("Serbia"),
    RU("Russland"),
    RW("Rwanda"),
    SA("Saudi-Arabia"),
    SB("Salomonøyene"),
    SC("Seychellene"),
    SD("Sudan"),
    SE("Sverige"),
    SG("Singapore"),
    SH("St. Helena, Ascension og Tristan da Cunha"),
    SI("Slovenia"),
    SJ("Svalbard og Jan Mayen"),
    SK("Slovakia"),
    SL("Sierra Leone"),
    SM("San Marino"),
    SN("Senegal"),
    SO("Somalia"),
    SR("Surinam"),
    SS("Sør-Sudan"),
    ST("São Tomé og Príncipe"),
    SV("El Salvador"),
    SX("Sint Maarten"),
    SY("Syria"),
    SZ("Swaziland"),
    TC("Turks- og Caicosøyene"),
    TD("Tsjad"),

    // Ikke inkludert i felles kodeverk
    // TF("De franske sørterritorier"),
    TG("Togo"),
    TH("Thailand"),
    TJ("Tadsjikistan"),
    TK("Tokelau"),
    TL("Øst-Timor"),
    TM("Turkmenistan"),
    TN("Tunisia"),
    TO("Tonga"),
    TR("Tyrkia"),
    TT("Trinidad og Tobago"),
    TV("Tuvalu"),
    TW("Taiwan"),
    TZ("Tanzania"),
    UA("Ukraina"),
    UG("Uganda"),
    UM("USAs ytre småøyer"),
    US("USA"),
    UY("Uruguay"),
    UZ("Usbekistan"),
    VA("Vatikanstaten"),
    VC("Saint Vincent og Grenadinene"),
    VE("Venezuela"),
    VG("De britiske Jomfruøyer"),
    VI("De amerikanske Jomfruøyer"),
    VN("Vietnam"),
    VU("Vanuatu"),
    WF("Wallis og Futuna"),
    WS("Samoa"),

    // X* koder er "brukerkoder".
    // SSB og felles kodeverk definerer XB Kanariøyene og XK Kosovo.
    XB("Kanariøyene"),
    XK("Kosovo"),

    YE("Jemen"),
    YT("Mayotte"),
    ZA("Sør-Afrika"),
    ZM("Zambia"),
    ZW("Zimbabwe"),
}
