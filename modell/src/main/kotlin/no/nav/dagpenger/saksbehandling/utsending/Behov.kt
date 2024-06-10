package no.nav.dagpenger.saksbehandling.utsending

import de.slub.urn.URN
import java.util.Base64

interface Behov {
    val navn: String
    val data: Map<String, Any>
}

data class ArkiverbartBrevBehov(
    private val html: String,
) : Behov {
    companion object {
        const val BEHOV_NAVN = "ArkiverbartBrevBehov"
    }

    override val navn: String = BEHOV_NAVN

    init {
        require(html.isNotBlank()) { "HTML kan ikke være tom" }
        // TODO: Add more validation of html
    }

    override val data: Map<String, Any> = mapOf("html" to html.toBase64())

    private fun String.toBase64() = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))
}

data class MidlertidigJournalføringBehov(
    private val pdfUrn: URN,
) : Behov {
    companion object {
        const val BEHOV_NAVN = "MidlertidigJournalføringBehov"
    }

    override val navn: String = BEHOV_NAVN
    override val data: Map<String, Any>
        get() =
            mapOf(
                "pdfUrn" to pdfUrn.toString(),
            )
}

data class DistribueringBehov(
    private val pdfUrn: URN,
) : Behov {
    companion object {
        const val BEHOV_NAVN = "DistribueringBehov"
    }

    override val navn: String = BEHOV_NAVN
    override val data: Map<String, Any>
        get() =
            mapOf(
                "pdfUrn" to pdfUrn.toString(),
            )
}

object IngenBehov : Behov {
    override val navn = "IngenBehov"
    override val data = emptyMap<String, Any>()
}
