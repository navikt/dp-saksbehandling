package no.nav.dagpenger.saksbehandling.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import java.time.LocalDateTime

data class OpprettGenerellOppgaveHendelse(
    val ident: String,
    val emneknagg: String,
    val tittel: String,
    val beskrivelse: String = "",
    val strukturertData: JsonNode = NullNode.instance,
    val registrertTidspunkt: LocalDateTime = LocalDateTime.now(),
    override val utførtAv: Behandler = Applikasjon.DpSaksbehandling,
) : Hendelse(utførtAv)
