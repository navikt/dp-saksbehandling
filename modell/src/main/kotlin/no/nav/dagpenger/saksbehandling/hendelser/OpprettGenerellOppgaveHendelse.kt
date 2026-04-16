package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import java.time.LocalDate
import java.time.LocalDateTime

data class OpprettGenerellOppgaveHendelse(
    val ident: String,
    val aarsak: String,
    val tittel: String,
    val beskrivelse: String = "",
    val strukturertData: Map<String, Any> = emptyMap(),
    val frist: LocalDate? = null,
    val beholdOppgaven: Boolean = false,
    val registrertTidspunkt: LocalDateTime = LocalDateTime.now(),
    override val utførtAv: Behandler = Applikasjon.DpSaksbehandling,
) : Hendelse(utførtAv)
