package no.nav.dagpenger.saksbehandling

sealed interface Behandler

data class Saksbehandler(
    val navIdent: String,
    val grupper: Set<String>,
    val tilganger: Set<TilgangType> = emptySet(),
) : Behandler

sealed class Applikasjon(
    val navn: String,
) : Behandler {
    companion object {
        fun fra(navn: String): Applikasjon =
            when (navn) {
                "dp-behandling" -> DpBehandling
                "dp-saksbehandling" -> DpSaksbehandling
                "dp-mottak" -> DpMottak
                "dp-kabal-integrasjon" -> DpKabalIntegrasjon
                "Kabal" -> Kabal
                "Tilbakekreving" -> Tilbakekreving
                else -> Generell(navn)
            }
    }

    object DpBehandling : Applikasjon(navn = "dp-behandling")

    object DpSaksbehandling : Applikasjon(navn = "dp-saksbehandling")

    object DpMottak : Applikasjon(navn = "dp-mottak")

    object DpKabalIntegrasjon : Applikasjon(navn = "dp-kabal-integrasjon")

    object Kabal : Applikasjon(navn = "Kabal")

    object Tilbakekreving : Applikasjon(navn = "Tilbakekreving")

    class Generell(
        navn: String,
    ) : Applikasjon(navn)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Applikasjon) return false

        if (navn != other.navn) return false

        return true
    }

    override fun hashCode(): Int = navn.hashCode()

    override fun toString(): String = "Applikasjon(navn='$navn')"
}
