package no.nav.dagpenger.saksbehandling

data class UtsendingSak(
    val id: String,
    val kontekst: String = "Arena",
) {
    fun toMap(): Map<String, String> = mapOf("id" to id, "kontekst" to kontekst)
}
