package no.nav.dagpenger.saksbehandling.mottak

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.util.UUID

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }

fun JsonNode.textOrNull(): String? =
    if (this.isMissingOrNull()) {
        null
    } else {
        this.asText()
    }

fun JsonNode.uuidOrNull(): UUID? = this.textOrNull()?.let { UUID.fromString(it) }
