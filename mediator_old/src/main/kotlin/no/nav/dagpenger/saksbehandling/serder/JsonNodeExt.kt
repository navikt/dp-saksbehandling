package no.nav.dagpenger.saksbehandling.serder

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
