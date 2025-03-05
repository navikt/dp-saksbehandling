package no.nav.dagpenger.saksbehandling.mottak

import java.util.UUID

enum class OpplysningTyper(val opplysningTypeId: UUID?) {
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
}
