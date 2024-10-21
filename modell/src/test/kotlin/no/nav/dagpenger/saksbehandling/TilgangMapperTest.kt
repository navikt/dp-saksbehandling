package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TilgangMapperTest {
    private val saksbehandlerGruppe = "saksbehandlerGruppe"
    private val beslutteGruppe = "beslutteGruppe"
    private val egneAnsatteGruppe = "egneAnsatteGruppe"
    private val fortroligAdresseGruppe = "fortroligAdresseGruppe"
    private val strengtFortroligAdresseGruppe = "strengtFortroligAdresseGruppe"
    private val strengtFortroligAdresseUtlandGruppe = "strengtFortroligAdresseUtlandGruppe"

    private val tilgangMapper =
        TilgangMapper(
            saksbehandlerGruppe,
            beslutteGruppe,
            egneAnsatteGruppe,
            fortroligAdresseGruppe,
            strengtFortroligAdresseGruppe,
            strengtFortroligAdresseUtlandGruppe,
        )

    @Test
    fun `skal mappe grupper til riktig TilgangType`() {
        val grupper =
            listOf(
                saksbehandlerGruppe,
                beslutteGruppe,
                egneAnsatteGruppe,
                fortroligAdresseGruppe,
                strengtFortroligAdresseGruppe,
                strengtFortroligAdresseUtlandGruppe,
                "ukjent",
            )

        val forventet =
            setOf(
                TilgangType.SAKSBEHANDLER,
                TilgangType.BESLUTTER,
                TilgangType.EGNE_ANSATTE,
                TilgangType.FORTROLIG_ADRESSE,
                TilgangType.STRENGT_FORTROLIG_ADRESSE,
                TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND,
            )

        tilgangMapper.map(grupper) shouldBe forventet
    }
}
