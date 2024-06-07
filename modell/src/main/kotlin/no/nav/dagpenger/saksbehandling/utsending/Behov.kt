package no.nav.dagpenger.saksbehandling.utsending

import java.util.Base64

interface Behov {
    val navn: String
    val data: Map<String, Any>
}

data class ArkiverbartBrevBehov(
    override val navn: String,
    private val html: String,
) : Behov {
    override val data: Map<String, Any>
        get() {
            return mapOf("html" to html.toBase64()) //
        }

    private fun String.toBase64() = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))

    // fun toJson() = mapOf("navn" to navn, "data" to data)
}

object IngenBehov : Behov {
    override val navn = "IngenBehov"
    override val data = emptyMap<String, Any>()
}
