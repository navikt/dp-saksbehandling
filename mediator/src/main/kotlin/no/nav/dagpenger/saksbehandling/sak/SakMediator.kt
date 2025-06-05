package no.nav.dagpenger.saksbehandling.sak

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.NyBehandling
import no.nav.dagpenger.saksbehandling.NyPerson
import no.nav.dagpenger.saksbehandling.NySak
import no.nav.dagpenger.saksbehandling.db.sak.NyPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse

class SakMediator(
    private val personRepository: NyPersonRepository,
    private val sakRepository: SakRepository,
) {
    fun opprettSak(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        val sak =
            NySak(
                søknadId = søknadsbehandlingOpprettetHendelse.søknadId,
                behandlinger =
                    listOf(
                        NyBehandling(
                            behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
                            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
                            opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
                        ),
                    ),
                opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
            )

        val person =
            personRepository.finn(søknadsbehandlingOpprettetHendelse.ident) ?: NyPerson(
                ident = søknadsbehandlingOpprettetHendelse.ident,
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
            ).also { it.leggTilSak(sak) }
        personRepository.lagre(person)
    }

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse) {
        TODO()
        // val person = personRepository.hent(meldekortbehandlingOpprettetHendelse.ident)
        // val sak = person.getSaker().find { it.behandlinger.contains(meldekortbehandlingOpprettetHendelse.behandlingId) }
    }
}
