package no.nav.dagpenger.saksbehandling.mottak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }

fun JsonMessage.folkeregisterIdent() = this["identer"].first { it["type"].asText() == "folkeregisterident" }["id"].asText()
