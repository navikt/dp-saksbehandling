package no.nav.dagpenger.behandling.hendelser.mottak

import com.fasterxml.jackson.databind.JsonNode
import mu.withLoggingContext
import no.nav.dagpenger.behandling.PersonMediator
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_resultat
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

internal class AldersbehovLøsningMottak(rapidsConnection: RapidsConnection, private val mediator: PersonMediator) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "prosess_resultat") }
            validate { it.requireKey("søknad_uuid", "resultat") }
            validate { it.requireValue("versjon_navn", "Paragraf_4_23_alder") }
            validate {
                it.requireArray("identer") {
                    requireKey("type", "historisk", "id")
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val ident = packet["identer"].first(gjeldendeFolkeregisterident)["id"].asText()
        val vilkårsvurderingId = packet["søknad_uuid"].asText().let { UUID.fromString(it) }

        withLoggingContext("vilkårsvurderingId" to vilkårsvurderingId.toString()) {
            val paragraf423AlderLøsning = Paragraf_4_23_alder_resultat(
                ident = ident,
                vilkårsvurderingId = vilkårsvurderingId,
                oppfylt = packet["resultat"].asBoolean()
            )

            mediator.behandle(paragraf423AlderLøsning)
        }
    }

    private val gjeldendeFolkeregisterident: (JsonNode) -> Boolean
        get() = { it["type"].asText() == "folkeregisterident" && !it["historisk"].asBoolean() }
}
