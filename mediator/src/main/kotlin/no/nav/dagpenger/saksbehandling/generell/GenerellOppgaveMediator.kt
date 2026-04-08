package no.nav.dagpenger.saksbehandling.generell

import PersonMediator
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.db.generell.GenerellOppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

class GenerellOppgaveMediator(
    private val generellOppgaveRepository: GenerellOppgaveRepository,
    private val personMediator: PersonMediator,
    private val generellOppgaveBehandler: GenerellOppgaveBehandler,
    private val sakMediator: SakMediator,
) {
    fun taImot(hendelse: OpprettGenerellOppgaveHendelse): GenerellOppgave {
        val person = personMediator.finnEllerOpprettPerson(hendelse.ident)

        val generellOppgave =
            GenerellOppgave.opprett(
                person = person,
                emneknagg = hendelse.emneknagg,
                tittel = hendelse.tittel,
                beskrivelse = hendelse.beskrivelse,
                strukturertData = hendelse.strukturertData,
            )

        generellOppgaveRepository.lagre(generellOppgave)
        logger.info { "Opprettet generell oppgave ${generellOppgave.id} med emneknagg ${generellOppgave.emneknagg}" }

        return generellOppgave
    }

    fun ferdigstill(hendelse: FerdigstillGenerellOppgaveHendelse) {
        val generellOppgave = hent(id = hendelse.generellOppgaveId, saksbehandler = hendelse.utførtAv)

        generellOppgave.startFerdigstilling(
            vurdering = hendelse.vurdering,
            valgtSakId = hendelse.aksjon.valgtSakId,
        )
        generellOppgaveRepository.lagre(generellOppgave)

        val ferdigstiltHendelse = generellOppgaveBehandler.utførAksjon(generellOppgave, hendelse)
        logger.info { "Ferdigstiller generell oppgave ${generellOppgave.id} med aksjon ${hendelse.aksjon.type}" }

        generellOppgave.ferdigstill(
            aksjonType = ferdigstiltHendelse.aksjonType,
            opprettetBehandlingId = ferdigstiltHendelse.opprettetBehandlingId,
        )
        generellOppgaveRepository.lagre(generellOppgave)
    }

    fun hent(
        id: UUID,
        saksbehandler: Saksbehandler,
    ): GenerellOppgave =
        generellOppgaveRepository.hent(id).also { oppgave ->
            oppgave.person.harTilgang(saksbehandler)
        }

    fun hentLovligeSaker(ident: String): List<Sak> = sakMediator.finnSakHistorikk(ident)?.saker() ?: emptyList()
}
