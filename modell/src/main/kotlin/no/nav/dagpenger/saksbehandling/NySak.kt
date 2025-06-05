package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import java.time.LocalDateTime
import java.util.UUID

data class NySak(
    val id: UUID = UUIDv7.ny(),
    val søknadId: UUID,
    val opprettet: LocalDateTime,
) {
    private val behandlinger: MutableList<NyBehandling> = mutableListOf()

    fun behandlinger(): List<NyBehandling> = behandlinger.toList()

    fun leggTilBehandling(behandling: NyBehandling) = behandlinger.add(behandling)

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse) {
        if (this.behandlinger.map { it.behandlingId }.containsAll(meldekortbehandlingOpprettetHendelse.basertPåBehandlinger)) {
            behandlinger.add(
                NyBehandling(
                    behandlingId = meldekortbehandlingOpprettetHendelse.behandlingId,
                    behandlingType = BehandlingType.MELDEKORT,
                    opprettet = meldekortbehandlingOpprettetHendelse.opprettet,
                ),
            )
        }
    }
}

data class NyBehandling(
    val behandlingId: UUID,
    val behandlingType: BehandlingType,
    val opprettet: LocalDateTime,
    val oppgave: UUID? = null,
)

class NyPerson(
    val id: UUID = UUIDv7.ny(),
    val ident: String,
    val skjermesSomEgneAnsatte: Boolean,
    val adressebeskyttelseGradering: AdressebeskyttelseGradering,
) {
    private val saker: MutableSet<NySak> = mutableSetOf()

    init {
        require(ident.matches(Regex("[0-9]{11}"))) { "Person-ident må ha 11 siffer, fikk ${ident.length}" }
    }

    fun knyttTilSak(meldekortbehandlingOpprettetHendelse: MeldekortbehandlingOpprettetHendelse) {
        saker.forEach { it.knyttTilSak(meldekortbehandlingOpprettetHendelse) }
    }

    fun leggTilSak(sak: NySak) = saker.add(sak)

    fun saker(): List<NySak> = saker.toList()
}
