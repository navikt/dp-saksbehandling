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
    init {
        require(html.isNotBlank()) { "HTML kan ikke være tom" }
        // TODO: Add more validation of html
    }

    override val data: Map<String, Any> = mapOf("html" to html.toBase64())

    private fun String.toBase64() = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))
}

object IngenBehov : Behov {
    override val navn = "IngenBehov"
    override val data = emptyMap<String, Any>()
}
