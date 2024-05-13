package no.nav.dagpenger.saksbehandling.mottak

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }

fun JsonNode?.avklaringstyper(): Set<String> {
    if (this == null) return emptySet()

    return this.filter {
        it["type"].erTekst()
    }.map { it["type"].asText() }.toSet()
}

private fun JsonNode?.erTekst(): Boolean = this != null && this.isTextual
