package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import java.time.LocalDateTime

data class OpprettGenerellOppgaveHendelse(
    val ident: String,
    val emneknagg: String,
    val tittel: String,
    val beskrivelse: String = "",
    val strukturertData: Map<String, Any> = emptyMap(),
    val registrertTidspunkt: LocalDateTime = LocalDateTime.now(),
    override val utførtAv: Behandler = Applikasjon.DpSaksbehandling,
) : Hendelse(utførtAv)
