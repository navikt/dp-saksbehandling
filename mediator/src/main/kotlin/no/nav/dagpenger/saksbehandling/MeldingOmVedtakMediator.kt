package no.nav.dagpenger.saksbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class MeldingOmVedtakMediator(
    private val oppgaveRepository: OppgaveRepository,
    private val meldingOmVedtakKlient: MeldingOmVedtakKlient,
    private val oppslag: Oppslag,
    private val sakMediator: SakMediator,
) {
    suspend fun hentMeldingOmVedtakHtml(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ): String {
        val oppgave = hentOppgaveMedTilgangskontroll(oppgaveId, saksbehandler)

        return coroutineScope {
            val personDeferred = async(Dispatchers.IO) { oppslag.hentPerson(oppgave.personIdent()) }
            val saksbehandlerDeferred = async(Dispatchers.IO) { oppslag.hentBehandler(saksbehandler.navIdent) }
            val beslutterDeferred =
                async(Dispatchers.IO) {
                    oppgave.sisteBeslutter()?.let { oppslag.hentBehandler(it) }
                }
            val sakIdDeferred =
                async(Dispatchers.IO) {
                    sakMediator.hentSakIdForBehandlingId(oppgave.behandling.behandlingId)
                }

            meldingOmVedtakKlient
                .hentMeldingOmVedtakHtml(
                    person = personDeferred.await(),
                    saksbehandler = saksbehandlerDeferred.await(),
                    beslutter = beslutterDeferred.await(),
                    behandlingId = oppgave.behandling.behandlingId,
                    saksbehandlerToken = saksbehandlerToken,
                    utløstAvType = oppgave.behandling.utløstAv,
                    sakId = sakIdDeferred.await()?.toString(),
                ).getOrThrow()
        }
    }

    suspend fun lagreUtvidetBeskrivelse(
        oppgaveId: UUID,
        brevblokkId: String,
        tekst: String,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ): String {
        val oppgave = hentOppgaveMedTilgangskontroll(oppgaveId, saksbehandler)

        return meldingOmVedtakKlient.lagreUtvidetBeskrivelse(
            behandlingId = oppgave.behandling.behandlingId,
            brevblokkId = brevblokkId,
            tekst = tekst,
            saksbehandlerToken = saksbehandlerToken,
        )
    }

    suspend fun lagreBrevVariant(
        oppgaveId: UUID,
        brevVariant: String,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        val oppgave = hentOppgaveMedTilgangskontroll(oppgaveId, saksbehandler)

        meldingOmVedtakKlient.lagreBrevVariant(
            behandlingId = oppgave.behandling.behandlingId,
            brevVariant = brevVariant,
            saksbehandlerToken = saksbehandlerToken,
        )
    }

    private fun hentOppgaveMedTilgangskontroll(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave =
        oppgaveRepository.hentOppgave(oppgaveId).also { oppgave ->
            oppgave.egneAnsatteTilgangskontroll(saksbehandler)
            oppgave.adressebeskyttelseTilgangskontroll(saksbehandler)
        }
}
