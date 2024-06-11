package no.nav.dagpenger.saksbehandling.utsending

import de.slub.urn.URN
import java.util.Base64
import java.util.UUID

interface Behov {
    val navn: String
    val data: Map<String, Any>
}

abstract class AbstractBehov(open val oppgaveId: UUID) : Behov {
    protected fun data() = mapOf("oppgaveId" to oppgaveId.toString())
}

data class ArkiverbartBrevBehov(
    override val oppgaveId: UUID,
    private val html: String,
) : AbstractBehov(oppgaveId) {
    companion object {
        const val BEHOV_NAVN = "ArkiverbartDokumentBehov"
    }

    override val navn: String = BEHOV_NAVN

    init {
        require(html.isNotBlank()) { "HTML kan ikke være tom" }
        // TODO: Add more validation of html
    }

    override val data: Map<String, Any> = data() + mapOf("html" to html.toBase64())

    private fun String.toBase64() = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))
}

data class JournalføringBehov(
    override val oppgaveId: UUID,
    private val pdfUrn: URN,
) : AbstractBehov(oppgaveId) {
    companion object {
        const val BEHOV_NAVN = "JournalføringBehov"
    }

    override val navn: String = BEHOV_NAVN
    override val data: Map<String, Any> = data() + mapOf("pdfUrn" to pdfUrn.toString())
}

data class DistribueringBehov(
    override val oppgaveId: UUID,
    private val journalpostId: String,
) : AbstractBehov(oppgaveId) {
    companion object {
        const val BEHOV_NAVN = "DistribueringBehov"
    }

    override val navn: String = BEHOV_NAVN
    override val data: Map<String, Any> = data() + mapOf("journalpostId" to journalpostId)
}

object IngenBehov : Behov {
    override val navn = "IngenBehov"
    override val data = emptyMap<String, Any>()
}
