package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.behandling.BehandlingException
import no.nav.dagpenger.saksbehandling.serder.objectMapper

object Noe

internal fun BehandlingException.toNoe(): Noe? =
    try {
        val jsonNode = objectMapper.readTree(this.text)
        if (jsonNode.get("nåværendeTilstand").stringValue() == "TilBeslutning" &&
            jsonNode
                .get("operasjon")
                .stringValue() == "godkjenn"
        ) {
            Noe
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
