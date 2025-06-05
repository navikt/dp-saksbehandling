package no.nav.dagpenger.saksbehandling

import java.time.LocalDateTime
import java.util.UUID

data class NySak(
    val id: UUID = UUIDv7.ny(),
    val søknadId: UUID,
    val behandlinger: List<NyBehandling>,
    val opprettet: LocalDateTime,
)

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

    fun leggTilSak(sak: NySak) = saker.add(sak)

    fun getSaker(): List<NySak> = saker.toList()
}
