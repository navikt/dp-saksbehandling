package no.nav.dagpenger.saksbehandling.mottak

import java.util.UUID

enum class OpplysningTyper(val opplysningTypeId: UUID?) {
    RettighetDagpegnerUnderPermittering(
        opplysningTypeId = UUID.fromString("0194881f-9444-7a73-a458-0af81c034d86"),
    ),
    RettighetOrdinæreDagpenger(
        opplysningTypeId = UUID.fromString("0194881f-9444-7a73-a458-0af81c034d8a"),
    ),
    RettighetDagpengerUnderPermitteringIFiskeforedlingsindustri(
        opplysningTypeId = UUID.fromString("0194881f-9444-7a73-a458-0af81c034d88"),
    ),
    RettighetDagpengerEtterKonkurs(
        opplysningTypeId = UUID.fromString("0194881f-9444-7a73-a458-0af81c034d87"),
    ),
    RettighetDagpengerEtterVerneplikt(
        opplysningTypeId = UUID.fromString("01948d43-e218-76f1-b29b-7e604241d98a"),
    ),
}
