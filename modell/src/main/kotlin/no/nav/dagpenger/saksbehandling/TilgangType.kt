package no.nav.dagpenger.saksbehandling

enum class TilgangType {
    SAKSBEHANDLER,
    BESLUTTER,
    EGNE_ANSATTE,
    FORTROLIG_ADRESSE,
    STRENGT_FORTROLIG_ADRESSE,
    STRENGT_FORTROLIG_ADRESSE_UTLAND,
}

class TilgangMapper(
    private val saksbehandlerGruppe: String,
    private val beslutteGruppe: String,
    private val egneAnsatteGruppe: String,
    private val fortroligAdresseGruppe: String,
    private val strengtFortroligAdresseGruppe: String,
    private val strengtFortroligAdresseUtlandGruppe: String,
) {
    fun map(grupper: List<String>): Set<TilgangType> {
        return grupper.mapNotNull {
            when (it) {
                saksbehandlerGruppe -> TilgangType.SAKSBEHANDLER
                beslutteGruppe -> TilgangType.BESLUTTER
                egneAnsatteGruppe -> TilgangType.EGNE_ANSATTE
                fortroligAdresseGruppe -> TilgangType.FORTROLIG_ADRESSE
                strengtFortroligAdresseGruppe -> TilgangType.STRENGT_FORTROLIG_ADRESSE
                strengtFortroligAdresseUtlandGruppe -> TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
                else -> null
            }
        }.toSet()
    }
}
