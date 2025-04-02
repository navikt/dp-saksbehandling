package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.Utfall.TomtUtfall
import java.util.UUID

class KlageBehandling(
    val id: UUID,
    val person: Person,
    val utfall: Utfall = TomtUtfall,
) {
    val grupper: Set<Gruppe> =
        setOf(
            Gruppe(GrupperNavn.FORMKRAV, emptySet()),
            Gruppe(GrupperNavn.KLAGESAK, emptySet()),
            Gruppe(GrupperNavn.FRIST, emptySet()),
        )
}

class Opplysning(
    val id: UUID,
    val navn: String,
    val verdi: Verdi = Verdi.TomVerdi,
    val gruppe: Gruppe,
)

enum class GrupperNavn {
    FORMKRAV,
    KLAGESAK,
    FRIST,
    KLAGE_ANKE,
}

class Gruppe(
    val navn: GrupperNavn,
    val opplysninger: Set<Opplysning>,
)

sealed class Verdi {
    object TomVerdi : Verdi()
}

sealed class Utfall {
    object TomtUtfall : Utfall()
}
