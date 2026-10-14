package no.nav.dagpenger.saksbehandling.db.oppgave

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.Oppgave
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class OppgaveOversikt(
    val oppgaveId: UUID,
    val behandlingId: UUID,
    val personIdent: String,
    val behandlerIdent: String?,
    val saksbehandlerIdent: String?,
    val beslutterIdent: String?,
    val tidspunktOpprettet: LocalDateTime,
    val utlostAv: HendelseBehandler,
    val emneknagger: Set<String>,
    val skjermesSomEgneAnsatte: Boolean,
    val adressebeskyttelseGradering: AdressebeskyttelseGradering,
    val tilstand: Oppgave.Tilstand.Type,
    val utsattTilDato: LocalDate?,
    val totaltFeilutbetaltBelop: Double?,
    val sendtTilKontroll: LocalDateTime?,
)

data class OppgaveOversiktResultat(
    val oppgaver: List<OppgaveOversikt>,
    val totaltAntallOppgaver: Int,
)
