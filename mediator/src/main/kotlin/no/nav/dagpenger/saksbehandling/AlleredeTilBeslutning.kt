package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.behandling.BehandlingException
import no.nav.dagpenger.saksbehandling.serder.objectMapper

object AlleredeTilBeslutning

internal fun BehandlingException.tilAlleredeTilBeslutning(): AlleredeTilBeslutning? =
    this.text?.let {
        runCatching {
            val jsonNode = objectMapper.readTree(it)
            if (jsonNode.path("nåværendeTilstand").stringValue() == "TilBeslutning" &&
                jsonNode.path("operasjon").stringValue() == "godkjenn"
            ) {
                AlleredeTilBeslutning
            } else {
                null
            }
        }.getOrNull()
    }
