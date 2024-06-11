package no.nav.dagpenger.saksbehandling

data class Sak(
    val id: String,
    val kontekst: String,
) {
    fun toMap(): Map<String, String> {
        return mapOf("id" to id, "kontekst" to kontekst)
    }
}
