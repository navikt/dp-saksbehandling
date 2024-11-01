package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Saksbehandler
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
        return tilstandslogg.filter {
            it.tilstand === Oppgave.Tilstand.Type.UNDER_KONTROLL
        }.map {
            Triple(it.id, it.hendelse.utførtAv, repository.finnNotat(it.id))
        }.filter { it.third != null }.map {
            OppgaveHistorikkDTO(
                type = OppgaveHistorikkDTO.Type.notat,
                tidspunkt = it.third!!.sistEndretTidspunkt,
                behandler =
                    OppgaveHistorikkBehandlerDTO(
                        navn = hentNavn((it.second as Saksbehandler).navIdent),
                        rolle = OppgaveHistorikkBehandlerDTO.Rolle.beslutter,
                    ),
                tittel = "Notat",
                body = it.third!!.hentTekst(),
            )
        }
    }

    private suspend fun hentNavn(ident: String): String {
        return saksbehandlerOppslag.hentSaksbehandler(ident).let {
            "${it.fornavn} ${it.etternavn}"
        }
    }
}
