package no.nav.dagpenger.saksbehandling.henvendelse

import PersonMediator
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.db.henvendelse.HenvendelseRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillHenvendelseHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.sak.SakMediator
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
        val henvendelse = henvendelseRepository.hent(hendelse.henvendelseId)
        val henvendelseBehandlet: HenvendelseFerdigstiltHendelse =
            henvendelseBehandler.utførAksjon(hendelse, henvendelse)
        henvendelse.ferdigstill(henvendelseBehandlet)
        henvendelseRepository.lagre(henvendelse = henvendelse)
    }
}
