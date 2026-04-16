package no.nav.dagpenger.saksbehandling.generell

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.generell.GenerellOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class OpprettetGenerellOppgave(
    val generellOppgaveId: UUID,
    val oppgaveId: UUID,
)

class GenerellOppgaveMediator(
    private val generellOppgaveRepository: GenerellOppgaveRepository,
    private val generellOppgaveBehandler: GenerellOppgaveBehandler,
    private val personMediator: PersonMediator,
    private val sakMediator: SakMediator,
    private val oppgaveMediator: OppgaveMediator,
) {
    fun taImot(hendelse: OpprettGenerellOppgaveHendelse): OpprettetGenerellOppgave {
        val generellOppgaveId = UUIDv7.ny()
        val person = personMediator.finnEllerOpprettPerson(hendelse.ident)

        // Tilgangskontroll for saksbehandler-opprettelse
        val saksbehandler = hendelse.utførtAv as? Saksbehandler
        if (saksbehandler != null) {
            person.harTilgang(saksbehandler)
        }

        val behandling =
            Behandling(
                behandlingId = generellOppgaveId,
                opprettet = hendelse.registrertTidspunkt,
                hendelse = hendelse,
                utløstAv = UtløstAvType.GENERELL,
            )

        sakMediator.lagreBehandling(
            personId = person.id,
            behandling = behandling,
        )

        val generellOppgave =
            GenerellOppgave.opprett(
                id = generellOppgaveId,
                person = person,
                tittel = hendelse.tittel,
                beskrivelse = hendelse.beskrivelse,
                strukturertData = hendelse.strukturertData,
                frist = hendelse.frist,
                opprettet = hendelse.registrertTidspunkt,
            )

        generellOppgaveRepository.lagre(generellOppgave)

        val oppgave =
            oppgaveMediator.lagOppgaveForGenerellOppgave(
                hendelse = hendelse,
                behandling = behandling,
                person = person,
                utsattTil = hendelse.frist,
            )

        val tilstandInfo = if (hendelse.frist != null) "i PåVent til ${hendelse.frist}" else "i KlarTilBehandling"
        logger.info { "Opprettet generell oppgave ${generellOppgave.id} med årsak ${hendelse.aarsak} $tilstandInfo" }

        if (hendelse.beholdOppgaven && saksbehandler != null) {
            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )
            logger.info { "Tildelte ny oppgave ${oppgave.oppgaveId} til ${saksbehandler.navIdent}" }
        }

        return OpprettetGenerellOppgave(
            generellOppgaveId = generellOppgave.id,
            oppgaveId = oppgave.oppgaveId,
        )
    }

    fun ferdigstill(hendelse: FerdigstillGenerellOppgaveHendelse) {
        val generellOppgave = hent(id = hendelse.generellOppgaveId, saksbehandler = hendelse.utførtAv)

        generellOppgave.startFerdigstilling(
            vurdering = hendelse.vurdering,
            valgtSakId = hendelse.aksjon.valgtSakId,
        )
        generellOppgaveRepository.lagre(generellOppgave)

        val ferdigstiltHendelse = generellOppgaveBehandler.utførAksjon(generellOppgave, hendelse, this)
        logger.info { "Ferdigstiller generell oppgave ${generellOppgave.id} med aksjon ${hendelse.aksjon.type}" }

        generellOppgave.ferdigstill(
            aksjonType = ferdigstiltHendelse.aksjonType,
            opprettetBehandlingId = ferdigstiltHendelse.opprettetBehandlingId,
        )
        generellOppgaveRepository.lagre(generellOppgave)

        oppgaveMediator.ferdigstillOppgave(ferdigstiltHendelse)

        if (ferdigstiltHendelse.beholdOppgaven) {
            val oppgaveId = ferdigstiltHendelse.opprettetOppgaveId
            if (oppgaveId != null) {
                oppgaveMediator.tildelOppgave(
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgaveId,
                        ansvarligIdent = hendelse.utførtAv.navIdent,
                        utførtAv = hendelse.utførtAv,
                    ),
                )
                logger.info { "Tildelte ny oppgave $oppgaveId til ${hendelse.utførtAv.navIdent}" }
            }
        }
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
