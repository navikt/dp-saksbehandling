package no.nav.dagpenger.saksbehandling.oppfolging

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.db.oppfolging.OppfølgingRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppfølgingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class OpprettetOppfølging(
    val oppfølgingId: UUID,
    val oppgaveId: UUID,
)

class OppfølgingMediator(
    private val transaksjoner: Transaksjoner,
    private val oppfølgingRepository: OppfølgingRepository,
    private val oppfølgingBehandler: OppfølgingBehandler,
    private val personMediator: PersonMediator,
    private val sakMediator: SakMediator,
    private val oppgaveMediator: OppgaveMediator,
) {
    fun taImot(
        hendelse: OpprettOppfølgingHendelse,
        ctx: Transaksjonskontekst = Transaksjonskontekst.IkkeAktiv,
    ): OpprettetOppfølging {
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
                utløstAv = HendelseBehandler.Intern.Oppfølging,
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

        val oppgave =
            transaksjoner.transaksjon(ctx) { aktiv ->
                sakMediator.lagreBehandling(
                    personId = person.id,
                    behandling = behandling,
                    ctx = aktiv,
                )
                oppfølgingRepository.lagre(oppfølging, aktiv)
                oppgaveMediator.lagOppgaveForOppfølging(
                    hendelse = hendelse,
                    behandling = behandling,
                    person = person,
                    ctx = aktiv,
                )
            }

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

        val ferdigstiltHendelse =
            when (hendelse.aksjon) {
                is OppfølgingAksjon.Avslutt ->
                    OppfølgingFerdigstiltHendelse(
                        oppfølgingId = oppfølging.id,
                        aksjonType = hendelse.aksjon.type,
                        opprettetBehandlingId = null,
                        utførtAv = hendelse.utførtAv,
                    )

                is OppfølgingAksjon.OpprettKlage ->
                    transaksjoner.transaksjon { ctx ->
                        oppfølgingBehandler.opprettKlage(oppfølging, hendelse, ctx)
                    }

                is OppfølgingAksjon.OpprettManuellBehandling,
                is OppfølgingAksjon.OpprettRevurderingBehandling,
                ->
                    oppfølgingBehandler.opprettBehandling(oppfølging, hendelse)

                is OppfølgingAksjon.OpprettOppfølging ->
                    transaksjoner.transaksjon { ctx ->
                        oppfølgingBehandler.opprettNyOppfølging(oppfølging, hendelse, this, ctx)
                    }
            }

        logger.info { "Ferdigstiller oppfølging ${oppfølging.id} med aksjon ${hendelse.aksjon.type}" }

        oppfølging.ferdigstill(
            aksjonType = ferdigstiltHendelse.aksjonType,
            opprettetBehandlingId = ferdigstiltHendelse.opprettetBehandlingId,
        )
        oppgaveMediator.ferdigstillOppgave(ferdigstiltHendelse)
        oppfølgingRepository.lagre(oppfølging)
    }

    fun hent(
        id: UUID,
        saksbehandler: Saksbehandler,
    ): Oppfølging =
        oppfølgingRepository.hent(id).also { oppfølging ->
            oppfølging.person.harTilgang(saksbehandler)
        }

    fun hentAlleSaker(ident: String): List<Sak> = sakMediator.finnSakHistorikk(ident)?.alleSaker() ?: emptyList()
}
