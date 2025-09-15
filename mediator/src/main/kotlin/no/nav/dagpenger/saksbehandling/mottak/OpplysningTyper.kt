package no.nav.dagpenger.saksbehandling.mottak

import java.util.UUID

enum class OpplysningTyper(val opplysningTypeId: UUID) {
    RETTIGHET_DAGPEGNER_UNDER_PERMITTERING(
        opplysningTypeId = UUID.fromString("0194881f-9444-7a73-a458-0af81c034d86"),
    ),
    RETTIGHET_ORDINÆRE_DAGPENGER(
        opplysningTypeId = UUID.fromString("0194881f-9444-7a73-a458-0af81c034d8a"),
    ),
    RETTIGHET_DAGPENGER_UNDER_PERMITTERING_I_FISKEFOREDLINGSINDUSTRI(
        opplysningTypeId = UUID.fromString("0194881f-9444-7a73-a458-0af81c034d88"),
    ),
    RETTIGHET_DAGPENGER_ETTER_KONKURS(
        opplysningTypeId = UUID.fromString("0194881f-9444-7a73-a458-0af81c034d87"),
    ),
    RETTIGHET_DAGPENGER_ETTER_VERNEPLIKT(
        opplysningTypeId = UUID.fromString("01948d43-e218-76f1-b29b-7e604241d98a"),
    ),
    OPPFYLLER_KRAV_TIL_MINSTEINNTEKT(
        opplysningTypeId = UUID.fromString("0194881f-9413-77ce-92ec-d29700f0424c"),
    ),
    KRAV_TIL_TAP_AV_ARBEIDSINNTEKT(
        opplysningTypeId = UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a761"),
    ),
    TAP_AV_ARBEIDSTID_ER_MINST_TERSKEL(
        opplysningTypeId = UUID.fromString("0194881f-9435-72a8-b1ce-9575cbc2a76e"),
    ),
    KRAV_TIL_ALDER(
        opplysningTypeId = UUID.fromString("0194881f-940b-76ff-acf5-ba7bcb367237"),
    ),
    IKKE_FULLE_YTELSER(
        opplysningTypeId = UUID.fromString("0194881f-943f-78d9-b874-00a4944c54f1"),
    ),
    OPPFYLLER_MEDLEMSKAP(
        opplysningTypeId = UUID.fromString("0194881f-9443-72b4-8b30-5f6cdb24d54d"),
    ),
    IKKE_PÅVIRKET_AV_STREIK_ELLER_LOCKOUT(
        opplysningTypeId = UUID.fromString("0194881f-91df-746a-a8ac-4a6b2b30685f"),
    ),
    OPPFYLLER_KRAVET_OPPHOLD(
        opplysningTypeId = UUID.fromString("0194881f-9443-72b4-8b30-5f6cdb24d54e"),
    ),
    KRAV_TIL_ARBEIDSSØKER(
        opplysningTypeId = UUID.fromString("0194881f-9442-707b-a6ee-e96c06877be2"),
    ),
    OPPYLLER_KRAV_TIL_REGISTRERT_ARBEIDSSØKER(
        opplysningTypeId = UUID.fromString("0194881f-9442-707b-a6ee-e96c06877be1"),
    ),
    OPPFYLLER_KRAV_TIL_IKKE_UTESTENGT(
        opplysningTypeId = UUID.fromString("0194881f-9447-7e36-a569-3e9f42bff9f7"),
    ),
    KRAV_TIL_UTDANNING_ELLER_OPPLÆRING(
        opplysningTypeId = UUID.fromString("0194881f-9445-734c-a7ee-045edf29b52d"),
    ),
}
