package no.nav.dagpenger.saksbehandling.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler

data class OpprettGenerellOppgaveHendelse(
    val ident: String,
    val oppgaveType: String,
    val tittel: String,
    val beskrivelse: String? = null,
    val strukturertData: JsonNode? = null,
    override val utførtAv: Behandler = Applikasjon.DpSaksbehandling,
) : Hendelse(utførtAv)
