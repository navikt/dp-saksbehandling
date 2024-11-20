package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkBehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.AnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
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
                    behandler = OppgaveHistorikkBehandlerDTO(
                        navn = hentNavn(tilstandsendring.hendelse.utførtAv)
                    ),
                    tittel = "statusendring",
                )
            )

            if (tilstandsendring.tilstand == UNDER_KONTROLL) {
                repository.finnNotat(tilstandsendring.id)?.let { notat ->
                    historikk.add(
                        OppgaveHistorikkDTO(
                            type = OppgaveHistorikkDTO.Type.notat,
                            tidspunkt = notat.sistEndretTidspunkt,
                            behandler = OppgaveHistorikkBehandlerDTO(
                                navn = hentNavn(tilstandsendring.hendelse.utførtAv)
                            ),
                            tittel = "Notat",
                            body = notat.hentTekst(),
                        )
                    )
                }
            }
        }
        return historikk
    }

    private suspend fun hentNavn(ident: String): String {
        return saksbehandlerOppslag.hentSaksbehandler(ident).let {
            "${it.fornavn} ${it.etternavn}"
        }
    }

    private suspend fun hentNavn(utførtAv: Behandler): String {
        return when (utførtAv) {
            is Saksbehandler -> hentNavn(utførtAv.navIdent)
            is Applikasjon -> utførtAv.navn
        }
    }
}




