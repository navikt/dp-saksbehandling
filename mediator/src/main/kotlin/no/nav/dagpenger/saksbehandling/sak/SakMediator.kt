package no.nav.dagpenger.saksbehandling.sak

import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.NyBehandling
import no.nav.dagpenger.saksbehandling.NySak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse

class SakMediator(
    private val personMediator: PersonMediator,
    private val sakRepository: SakRepository,
) {
    fun hentSakHistorikk(ident: String): SakHistorikk {
        return sakRepository.hentSakHistorikk(ident)
    }

    fun finnSak(ident: String): SakHistorikk? {
        return sakRepository.finn(ident)
    }

    fun opprettSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        val sak =
            NySak(
                søknadId = søknadsbehandlingOpprettetHendelse.søknadId,
                opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
            ).also {
                it.leggTilBehandling(
                    NyBehandling(
                        behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
                        behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
                        opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
                    ),
                )
            }

        val sakHistorikk =
            sakRepository.finn(søknadsbehandlingOpprettetHendelse.ident) ?: SakHistorikk(
                person = personMediator.finnEllerOpprettPerson(søknadsbehandlingOpprettetHendelse.ident),
            )

        sakHistorikk.leggTilSak(sak)
        sakRepository.lagre(sakHistorikk)
    }

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse) {
        sakRepository.hentSakHistorikk(meldekortbehandlingOpprettetHendelse.ident).knyttTilSak(
            meldekortbehandlingOpprettetHendelse = meldekortbehandlingOpprettetHendelse,
        )
    }
}
