package no.nav.dagpenger.saksbehandling.mottak

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }

fun JsonMessage.emneknagger(): Set<String> {
    return try {
        when (this["utfall"].asBoolean()) {
            true -> setOf("Innvilgelse")
            false -> {
                if (this["harAvklart"].asText() == "Krav til minsteinntekt") {
                    return setOf("Avslag minsteinntekt")
                } else {
                    return setOf("Avslag")
                }
            }
        }
    } catch (e: NullPointerException) {
        logger.warn { "Fant ikke utfall eller harAvklart. Lager ingen emneknagger." }
        return emptySet()
    }
}

// private fun JsonNode?.erTekst(): Boolean = this != null && this.isTextual
