package no.nav.dagpenger.saksbehandling.oppfolging

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.oppfolging.OppfølgingRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class OpprettetOppfølging(
    val oppfølgingId: UUID,
    val oppgaveId: UUID,
)

class OppfølgingMediator(
    private val oppfølgingRepository: OppfølgingRepository,
    private val oppfølgingBehandler: OppfølgingBehandler,
    private val personMediator: PersonMediator,
    private val sakMediator: SakMediator,
    private val oppgaveMediator: OppgaveMediator,
) {
    fun taImot(hendelse: OpprettOppfølgingHendelse): OpprettetOppfølging {
        val oppfølgingId = UUIDv7.ny()
        val person = personMediator.finnEllerOpprettPerson(hendelse.ident)

        // Tilgangskontroll for saksbehandler-opprettelse
        val saksbehandler = hendelse.utførtAv as? Saksbehandler
        if (saksbehandler != null) {
            person.harTilgang(saksbehandler)
        }

        val behandling =
            Behandling(
                behandlingId = oppfølgingId,
                opprettet = hendelse.registrertTidspunkt,
                hendelse = hendelse,
                utløstAv = UtløstAvType.OPPFØLGING,
            )

        sakMediator.lagreBehandling(
            personId = person.id,
            behandling = behandling,
        )

        val oppfølging =
            Oppfølging.opprett(
                id = oppfølgingId,
                person = person,
                tittel = hendelse.tittel,
                beskrivelse = hendelse.beskrivelse,
                strukturertData = hendelse.strukturertData,
                frist = hendelse.frist,
                opprettet = hendelse.registrertTidspunkt,
            )

        oppfølgingRepository.lagre(oppfølging)

        val oppgave =
            oppgaveMediator.lagOppgaveForOppfølging(
                hendelse = hendelse,
                behandling = behandling,
                person = person,
            )

        logger.info {
            "Opprettet oppfølging ${oppfølging.id} med årsak ${hendelse.aarsak} i tilstand ${oppgave.tilstand()}" +
                (oppgave.utsattTil()?.let { " med frist $it" } ?: "")
        }

        return OpprettetOppfølging(
            oppfølgingId = oppfølging.id,
            oppgaveId = oppgave.oppgaveId,
        )
    }

    fun ferdigstill(hendelse: FerdigstillOppfølgingHendelse) {
        val oppfølging = hent(id = hendelse.oppfølgingId, saksbehandler = hendelse.utførtAv)

        oppfølging.startFerdigstilling(
            vurdering = hendelse.vurdering,
            valgtSakId = hendelse.aksjon.valgtSakId,
        )
        oppfølgingRepository.lagre(oppfølging)

        val ferdigstiltHendelse = oppfølgingBehandler.utførAksjon(oppfølging, hendelse, this)
        logger.info { "Ferdigstiller oppfølging ${oppfølging.id} med aksjon ${hendelse.aksjon.type}" }

        oppfølging.ferdigstill(
            aksjonType = ferdigstiltHendelse.aksjonType,
            opprettetBehandlingId = ferdigstiltHendelse.opprettetBehandlingId,
        )
        oppgaveMediator.ferdigstillOppgave(ferdigstiltHendelse)
        oppfølgingRepository.lagre(oppfølging)

        if (ferdigstiltHendelse.beholdOppgaven) {
            val oppgaveId =
                requireNotNull(ferdigstiltHendelse.opprettetOppgaveId) {
                    "opprettetOppgaveId må være satt når beholdOppgaven=true"
                }
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

    fun hent(
        id: UUID,
        saksbehandler: Saksbehandler,
    ): Oppfølging =
        oppfølgingRepository.hent(id).also { oppfølging ->
            oppfølging.person.harTilgang(saksbehandler)
        }

    fun hentLovligeSaker(ident: String): List<Sak> = sakMediator.finnSakHistorikk(ident)?.saker() ?: emptyList()
}
