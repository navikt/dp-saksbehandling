package no.nav.dagpenger.saksbehandling.utsending

import de.slub.urn.URN
import no.nav.dagpenger.saksbehandling.UtsendingSak
import java.util.Base64
import java.util.UUID

sealed class Behov {
    abstract val oppgaveId: UUID
    abstract val navn: String
    protected abstract val data: Map<String, Any>

    fun data() = mapOf("oppgaveId" to oppgaveId.toString()) + data
}

data class ArkiverbartBrevBehov(
    override val oppgaveId: UUID,
    private val html: String,
    private val ident: String,
    private val utsendingSak: UtsendingSak,
) : Behov() {
    companion object {
        const val BEHOV_NAVN = "SaksbehandlingPdfBehov"
    }

    override val navn: String = BEHOV_NAVN

    init {
        require(html.isNotBlank()) { "HTML kan ikke være tom" }
        // TODO: Add more validation of html
    }

    override val data: Map<String, Any> =
        mapOf(
            "htmlBase64" to html.toBase64(),
            "ident" to ident,
            "dokumentNavn" to "vedtak.pdf",
            "kontekst" to "oppgave/$oppgaveId",
            "sak" to
                mapOf(
                    "id" to utsendingSak.id,
                    "kontekst" to utsendingSak.kontekst,
                ),
        )

    private fun String.toBase64() = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))
}

data class JournalføringBehov(
    override val oppgaveId: UUID,
    private val pdfUrn: URN,
    private val ident: String,
    private val utsendingSak: UtsendingSak,
    private val utsendingType: UtsendingType,
) : Behov() {
    companion object {
        const val BEHOV_NAVN = "JournalføringBehov"
    }

    override val navn: String = BEHOV_NAVN
    override val data: Map<String, Any> =
        mapOf(
            "pdfUrn" to pdfUrn.toString(),
            "ident" to ident,
            "tittel" to utsendingType.brevTittel,
            "skjemaKode" to utsendingType.skjemaKode,
            "sak" to
                mapOf(
                    "id" to utsendingSak.id,
                    "kontekst" to utsendingSak.kontekst,
                ),
        )
}

data class DistribueringBehov(
    override val oppgaveId: UUID,
    private val journalpostId: String,
) : Behov() {
    companion object {
        const val BEHOV_NAVN = "DistribueringBehov"
    }

    override val navn: String = BEHOV_NAVN
    override val data: Map<String, Any> = mapOf("journalpostId" to journalpostId)
}

object IngenBehov : Behov() {
    override val oppgaveId: UUID
        get() = throw NotImplementedError("Ingen behov har ingen oppgaveId")
    override val navn = "IngenBehov"
    override val data = emptyMap<String, Any>()
}
