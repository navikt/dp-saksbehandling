package no.nav.dagpenger.behandling.hendelser.mottak

import mu.withLoggingContext
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Aldersbehov
import no.nav.dagpenger.behandling.PersonMediator
import no.nav.dagpenger.behandling.hendelser.AldersvilkårLøsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

internal class AldersbehovLøsningMottak(rapidsConnection: RapidsConnection, private val mediator: PersonMediator) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.requireKey("behandlingId", "ident", "@behovId") }
            validate { it.demandAllOrAny("@behov", listOf(Aldersbehov.name)) }
            validate { it.requireKey("@løsning") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val ident = packet["ident"].asText()
        val behandlingId = packet["behandlingId"].asText().let { UUID.fromString(it) }
        val resultat = packet["@løsning"][Aldersbehov.name].asBoolean()

        withLoggingContext("behandlingId" to behandlingId.toString()) {
            val aldersvilkårLøsning = AldersvilkårLøsning(
                ident = ident,
                behandlingId = behandlingId,
                oppfylt = resultat
            )

            mediator.behandle(aldersvilkårLøsning)
        }
    }
}
