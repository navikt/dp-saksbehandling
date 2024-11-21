package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.BEHANDLES_I_ARENA
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkBehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag

internal class OppgaveHistorikkDTOMapper(
    private val repository: OppgaveRepository,
    private val saksbehandlerOppslag: SaksbehandlerOppslag,
) {
    suspend fun lagOppgaveHistorikk(tilstandslogg: Tilstandslogg): List<OppgaveHistorikkDTO> {
        val historikk = mutableListOf<OppgaveHistorikkDTO>()
        tilstandslogg.forEach { tilstandsendring ->
            historikk.add(
                OppgaveHistorikkDTO(
                    type = OppgaveHistorikkDTO.Type.statusendring,
                    tidspunkt = tilstandsendring.tidspunkt,
                    tittel = tilstandsendringTittel(tilstandsendring),
                    behandler =
                        OppgaveHistorikkBehandlerDTO(
                            navn = hentNavn(tilstandsendring.hendelse.utførtAv),
                        ),
                ),
            )

            if (tilstandsendring.tilstand == UNDER_KONTROLL) {
                repository.finnNotat(tilstandsendring.id)?.let { notat ->
                    historikk.add(
                        OppgaveHistorikkDTO(
                            type = OppgaveHistorikkDTO.Type.notat,
                            tidspunkt = notat.sistEndretTidspunkt,
                            tittel = "Notat",
                            behandler =
                                OppgaveHistorikkBehandlerDTO(
                                    navn = hentNavn(tilstandsendring.hendelse.utførtAv),
                                ),
                            body = notat.hentTekst(),
                        ),
                    )
                }
            }
        }
        return historikk
    }

    private fun tilstandsendringTittel(tilstandsendring: Tilstandsendring): String {
        return "Ny status: " +
            when (tilstandsendring.tilstand) {
                OPPRETTET -> "Opprettet"
                KLAR_TIL_BEHANDLING -> "Klar til behandling"
                UNDER_BEHANDLING -> "Under behandling"
                FERDIG_BEHANDLET -> "Ferdig behandlet"
                PAA_VENT -> "På vent"
                KLAR_TIL_KONTROLL -> "Klar til kontroll"
                UNDER_KONTROLL -> "Under kontroll"
                AVVENTER_LÅS_AV_BEHANDLING -> "Sendt til kontroll"
                AVVENTER_OPPLÅSING_AV_BEHANDLING -> "Returnert til saksbehandling"
                BEHANDLES_I_ARENA -> "Behandles i Arena"
            }
    }

    private suspend fun hentNavn(ident: String): String {
        return saksbehandlerOppslag.hentSaksbehandler(ident).let {
            "${it.etternavn}, ${it.fornavn}"
        }
    }

    private suspend fun hentNavn(utførtAv: Behandler): String {
        return when (utførtAv) {
            is Saksbehandler -> hentNavn(utførtAv.navIdent)
            is Applikasjon -> utførtAv.navn
        }
    }
}
