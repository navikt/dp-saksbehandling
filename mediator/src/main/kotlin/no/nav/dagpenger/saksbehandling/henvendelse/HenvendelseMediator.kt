package no.nav.dagpenger.saksbehandling.henvendelse

import PersonMediator
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.db.henvendelse.HenvendelseRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillHenvendelseHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.tilgangsstyring.SaksbehandlerErIkkeEier
import java.util.UUID

sealed class HåndterHenvendelseResultat {
    data class HåndtertHenvendelse(val sakId: UUID) : HåndterHenvendelseResultat()

    object UhåndtertHenvendelse : HåndterHenvendelseResultat()
}

class HenvendelseMediator(
    private val sakMediator: SakMediator,
    private val oppgaveMediator: OppgaveMediator,
    private val personMediator: PersonMediator,
    private val henvendelseRepository: HenvendelseRepository,
    private val henvendelseBehandler: HenvendelseBehandler,
) {
    fun taImotHenvendelse(hendelse: HenvendelseMottattHendelse): HåndterHenvendelseResultat {
        val skalEttersendingTilSøknadVarsles =
            hendelse.kategori == Kategori.ETTERSENDING && hendelse.søknadId != null &&
                oppgaveMediator.skalEttersendingTilSøknadVarsles(
                    søknadId = hendelse.søknadId!!,
                    ident = hendelse.ident,
                )

        val sisteSakId = sakMediator.finnSisteSakId(hendelse.ident)

        if (skalEttersendingTilSøknadVarsles || sisteSakId != null) {
            val henvendelse =
                Henvendelse.opprett(hendelse = hendelse) { ident ->
                    personMediator.finnEllerOpprettPerson(ident)
                }
            henvendelseRepository.lagre(henvendelse = henvendelse)
        }
        return when (sisteSakId) {
            null -> HåndterHenvendelseResultat.UhåndtertHenvendelse
            else -> HåndterHenvendelseResultat.HåndtertHenvendelse(sisteSakId)
        }
    }

    fun ferdigstill(hendelse: FerdigstillHenvendelseHendelse) {
        val henvendelse = hentHenvendelse(henvendelseId = hendelse.henvendelseId, behandler = hendelse.utførtAv)
        requireEierskap(henvendelse = henvendelse, behandler = hendelse.utførtAv)
        val henvendelseBehandlet: HenvendelseFerdigstiltHendelse =
            henvendelseBehandler.utførAksjon(hendelse, henvendelse)
        henvendelse.ferdigstill(henvendelseBehandlet)
        henvendelseRepository.lagre(henvendelse = henvendelse)
    }

    fun avbrytHenvendelse(hendelse: BehandlingOpprettetForSøknadHendelse) {
        val henvendelser = henvendelseRepository.finnHenvendelserForPerson(ident = hendelse.ident)
        henvendelser.singleOrNull {
            it.tilstand().type == KLAR_TIL_BEHANDLING && it.gjelderSøknadMedId(søknadId = hendelse.søknadId)
        }
            ?.let { henvendelse ->
                henvendelse.avbryt(hendelse)
                henvendelseRepository.lagre(henvendelse = henvendelse)
            }
    }

    fun tildel(tildelHendelse: TildelHendelse) {
        hentHenvendelse(
            henvendelseId = tildelHendelse.henvendelseId,
            behandler = tildelHendelse.utførtAv,
        ).let {
            it.tildel(tildelHendelse)
            henvendelseRepository.lagre(henvendelse = it)
        }
    }

    fun hentHenvendelse(
        henvendelseId: UUID,
        behandler: Behandler,
    ): Henvendelse {
        return henvendelseRepository.hent(henvendelseId).also { henvendelse ->
            henvendelse.harTilgang(behandler)
        }
    }

    private fun requireEierskap(
        henvendelse: Henvendelse,
        behandler: Behandler,
    ) {
        when (behandler) {
            is Applikasjon -> {}
            is Saksbehandler -> {
                if (henvendelse.behandlerIdent() != behandler.navIdent) {
                    throw SaksbehandlerErIkkeEier(
                        "Saksbehandler ${behandler.navIdent} eier ikke " +
                            "henvendelsen ${henvendelse.henvendelseId}",
                    )
                }
            }
        }
    }
}
