package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.behandling.BehandlingException
import no.nav.dagpenger.saksbehandling.serder.objectMapper

object AlleredeTilBeslutning

internal fun BehandlingException.tilAlleredeTilBeslutning(): AlleredeTilBeslutning? =
    try {
        val jsonNode = objectMapper.readTree(this.text)
        if (jsonNode.get("nåværendeTilstand").stringValue() == "TilBeslutning" &&
            jsonNode
                .get("operasjon")
                .stringValue() == "godkjenn"
        ) {
            AlleredeTilBeslutning
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
