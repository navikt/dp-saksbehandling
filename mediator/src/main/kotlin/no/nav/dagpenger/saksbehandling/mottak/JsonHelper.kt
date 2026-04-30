package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import tools.jackson.databind.JsonNode
import java.util.UUID

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }

fun JsonNode.textOrNull(): String? =
    if (this.isMissingOrNull()) {
        null
    } else {
        this.asText()
    }

fun JsonNode.uuidOrNull(): UUID? = this.textOrNull()?.let { UUID.fromString(it) }
