package no.nav.dagpenger.saksbehandling

data class UtsendingSak(
    val id: String,
    val kontekst: String = "Arena",
) {
    fun toMap(): Map<String, String> {
        return mapOf("id" to id, "kontekst" to kontekst)
    }
}
