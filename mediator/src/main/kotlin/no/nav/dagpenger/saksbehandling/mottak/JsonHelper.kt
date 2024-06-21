package no.nav.dagpenger.saksbehandling.mottak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }

fun JsonMessage.emneknagger(): Set<String> {
    return try {
        this["avklaringer"].avklaringstyper()
    } catch (e: NullPointerException) {
        emptySet()
    }
}

fun JsonNode.sakId(): String {
    return this.single {
        val jsonNode = it["opplysningstype"]["id"]
        jsonNode.erTekst() && jsonNode.asText() == "fagsakId"
    }.let { fagsakNode ->
        fagsakNode["verdi"].asInt().toString()
    }
}

private fun JsonNode.avklaringstyper(): Set<String> {
    return this.filter {
        it["type"].erTekst()
    }.map { it["type"].asText() }.toSet()
}

private fun JsonNode?.erTekst(): Boolean = this != null && this.isTextual
