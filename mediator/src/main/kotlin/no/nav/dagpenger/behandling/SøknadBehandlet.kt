package no.nav.dagpenger.behandling

import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.UUID

class SøknadBehandlet(
    private val behandlingId: UUID,
    private val ident: String,
    private val virkningsdato: LocalDateTime = LocalDateTime.now(), // TODO: defaulter midlertidig
    private val innvilget: Boolean,
) {

    private val eventnavn = "søknad_behandlet_hendelse"

    fun behandlingId() = behandlingId
    fun ident() = ident
    fun innvilget() = innvilget

    fun toJson() = JsonMessage.newMessage(
        mapOf(
            "@event_name" to eventnavn,
            "behandlingId" to behandlingId,
            "virkningsdato" to virkningsdato,
            "innvilget" to innvilget,
            "ident" to ident,
        ),
    ).toJson()
}
